package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.entity.TcParams;
import com.zlt.aps.tc.mapper.TcCurlRollMapper;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.mapper.TcStockMapper;
import com.zlt.aps.tc.service.TcStockService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胎侧库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-31
 */
@Service
public class TcStockServiceImpl implements TcStockService {
    @Autowired
    private TcStockMapper tcStockMapper;

    @Autowired
    private TcCurlRollMapper curlRollMapper;

    @Autowired
    private TcParamsMapper paramsMapper;

    /**
     * 查询胎侧库存信息
     *
     * @param id 胎侧库存信息ID
     * @return 胎侧库存信息
     */
    @Override
    public TcStock selectTcStockById(Long id) {
        TcStock stock = tcStockMapper.selectTcStockById(id);
        LambdaQueryWrapper<TcCurlRoll> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcCurlRoll::getSidewallCode, stock.getMaterialCode());
        wrapper.eq(TcCurlRoll::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        TcCurlRoll curlRoll = curlRollMapper.selectOne(wrapper);
        if (curlRoll != null) {
            stock.setCurlLength(curlRoll.getCurlLength());
        }
        return stock;
    }

    /**
     * 查询胎侧库存信息列表
     *
     * @param tcStock 胎侧库存信息
     * @return 胎侧库存信息
     */
    @Override
    public List<TcStock> selectTcStockList(TcStock tcStock) {
        if (StringUtils.isNotEmpty(tcStock.getEndTime())) {
            tcStock.setEndTime(tcStock.getEndTime() + " 23:59:59");
        }
        List<TcStock> stockList = tcStockMapper.selectTcStockList(tcStock);
        if (CollectionUtils.isNotEmpty(stockList)) {
            List<String> codeList = stockList.stream().map(TcStock::getMaterialCode).distinct().collect(Collectors.toList());
            Map<String, BigDecimal> lengthMap = new HashMap<>(16);
            if (com.alibaba.nacos.common.utils.CollectionUtils.isNotEmpty(codeList)) {
                LambdaQueryWrapper<TcCurlRoll> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(TcCurlRoll::getSidewallCode, codeList);
                wrapper.eq(TcCurlRoll::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
                List<TcCurlRoll> curlRollList = curlRollMapper.selectList(wrapper);
                lengthMap = curlRollList.stream().collect(Collectors.toMap(TcCurlRoll::getSidewallCode, TcCurlRoll::getCurlLength));
            }
            for (TcStock stock : stockList) {
                String materialCode = stock.getMaterialCode();
                if (lengthMap.containsKey(materialCode)) {
                    BigDecimal length = lengthMap.get(materialCode);
                    stock.setCurlLength(length);
                }
            }
        }
        return stockList;
    }

    /**
     * 新增胎侧库存信息
     *
     * @param tcStock 胎侧库存信息
     * @return 结果
     */
    @Override
    public int insertTcStock(TcStock tcStock) {
        tcStock.setBaseVale(null);
        return tcStockMapper.insertTcStock(tcStock);
    }

    /**
     * 修改胎侧库存信息
     *
     * @param tcStock 胎侧库存信息
     * @return 结果
     */
    @Override
    public int updateTcStock(TcStock tcStock) {
        tcStock.setBaseVale(tcStock.getId());
        return tcStockMapper.updateTcStock(tcStock);
    }

    /**
     * 批量删除胎侧库存信息
     *
     * @param ids 需要删除的胎侧库存信息ID
     * @return 结果
     */
    @Override
    public int deleteTcStockByIds(Long[] ids) {
        return tcStockMapper.deleteTcStockByIds(ids);
    }

    /**
     * 删除胎侧库存信息信息
     *
     * @param id 胎侧库存信息ID
     * @return 结果
     */
    @Override
    public int deleteTcStockById(Long id) {
        return tcStockMapper.deleteTcStockById(id);
    }

    /**
     * 校验胎侧库存唯一性（根据库存日期+物料编号+id）
     */
    public List<TcStock> checkTcStockListUnic(TcStock tcStock) {
        return tcStockMapper.checkTcStockListUnic(tcStock);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<TcStock> list, boolean updateSupport, Long importLogId) {

        //初始化值准备
        int successNum = 0;
        int failureNum = 0;
        List<TcStock> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        LambdaUpdateWrapper<TcParams> paramsWrapper = new LambdaUpdateWrapper<>();
        paramsWrapper.eq(TcParams::getParamCode, "STANDARD_CRIMP_LENGTH");
        TcParams params = paramsMapper.selectOne(paramsWrapper);

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getStockDate()+a.getMaterialCode()), Collectors.counting()));

        Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
        List<TcCurlRoll> curlRollList = new ArrayList<>();
        List<String> codeList = list.stream().map(TcStock::getMaterialCode).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
        List<List<String>> splitList = CollectionUtil.splitList(codeList, 100);
        for (List<String> stringList : splitList) {
            LambdaUpdateWrapper<TcCurlRoll> wrapper = new LambdaUpdateWrapper<>();
            wrapper.in(TcCurlRoll::getSidewallCode, stringList);
            wrapper.eq(TcCurlRoll::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
            curlRollList.addAll(curlRollMapper.selectList(wrapper));
        }
        if (CollectionUtils.isNotEmpty(curlRollList)) {
            curlRollMap = curlRollList.stream().collect(Collectors.toMap(TcCurlRoll::getSidewallCode, TcCurlRoll::getCurlLength, (m1, m2) -> m1));
        }

        //校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            TcStock stock = list.get(i);

            String materialCode = stock.getMaterialCode();
            //重复记录校验
            Long hasValue = groupMap.get(stock.getStockDate()+materialCode);
            if (hasValue > 1) {
                failureNum++;
                stock.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.stock.stockDate");
                String columnName2 = I18nUtil.getMessage("ui.data.column.quota.sidewallCode");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            // 库存量(米)和库存量(卷)不能同时为空
            if (ObjectUtils.allNull(stock.getStockNum(), stock.getRollStockNum())) {
                failureNum++;
                stock.setId(-999L);
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.data.column.stock.stockNumAndRollNumNotNull"), importErrorLogs);
                continue;
            }

            // 卷数转换成米数，或米数转换成卷数
            /*if (!curlRollMap.containsKey(materialCode)) {
                failureNum++;
                stock.setId(-999L);
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.data.column.tc.stock.curlLengthNotExist"), importErrorLogs);
                continue;
            }*/

            BigDecimal curlLength = curlRollMap.getOrDefault(materialCode, new BigDecimal(params.getParamValue()));
            BigDecimal rollStockNum = stock.getRollStockNum();
            if (rollStockNum != null) {
                BigDecimal stockNum = rollStockNum.multiply(curlLength);
                stock.setStockNum(stockNum);
            } else {
                stock.setRollStockNum(stock.getStockNum().divide(curlLength, 2, RoundingMode.HALF_UP));
            }

            BigDecimal rollModifyNum = stock.getRollModifyNum();
            if (rollModifyNum != null) {
                BigDecimal modifyNum = rollModifyNum.multiply(curlLength);
                stock.setModifyNum(modifyNum);
            } else if (stock.getModifyNum() != null) {
                stock.setRollModifyNum(stock.getModifyNum().divide(curlLength, 2, RoundingMode.HALF_UP));
            }

            BigDecimal rollBadNum = stock.getRollBadNum();
            if (rollBadNum != null) {
                BigDecimal badNum = rollBadNum.multiply(curlLength);
                stock.setBadNum(badNum);
            } else if (stock.getBadNum() != null){
                stock.setRollBadNum(stock.getBadNum().divide(curlLength, 2, RoundingMode.HALF_UP));
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, stock);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                stock.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {

                BigDecimal StockNum =stock.getStockNum()==null?new BigDecimal(0):stock.getStockNum();
                BigDecimal ModifyNum =stock.getModifyNum()==null?new BigDecimal(0):stock.getModifyNum();
                BigDecimal BadNum =stock.getBadNum()==null?new BigDecimal(0):stock.getBadNum();
                BigDecimal dd= StockNum.add(ModifyNum).subtract(BadNum);
                if(dd.compareTo(new BigDecimal(0))<0){
                    failureNum++;
                    stock.setId(-999L);
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.stock.stockNumValidate"), importErrorLogs);
                    continue;
                }

                stock.setBaseVale(null);
                newList.add(stock);
            }
        }

        //新集合操作（更新或插入操作）
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                successNum = newList.size();
                tcStockMapper.mergeSql(newList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TcStock excelItem = list.get(i);
                    // 错误跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }

                    // 唯一性校验
                    List<TcStock> exist = tcStockMapper.checkTcStockListUnic(excelItem);
                    if (CollectionUtils.isEmpty(exist)) {
                        successNum++;
                        tcStockMapper.insertTcStock(excelItem);
                    } else {
                        failureNum++;
                        String message = I18nUtil.getMessage("ui.error.message.quota.unique");
                        ImportUtil.addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                        continue;
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 执行sql失败，插入导入失败记录
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            ImportUtil.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }

        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
