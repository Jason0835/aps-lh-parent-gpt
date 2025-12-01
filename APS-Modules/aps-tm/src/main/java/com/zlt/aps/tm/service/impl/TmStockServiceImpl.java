package com.zlt.aps.tm.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.aps.tm.entity.TmParams;
import com.zlt.aps.tm.mapper.TmCurlRollMapper;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import com.zlt.aps.tm.mapper.TmStockMapper;
import com.zlt.aps.tm.service.TmStockService;
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
 * 胎面库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-25
 */
@Service
public class TmStockServiceImpl implements TmStockService {

    @Autowired
    private TmStockMapper tTmStockMapper;

    @Autowired
    private TmCurlRollMapper tmCurlRollMapper;

    @Autowired
    private TmParamsMapper paramsMapper;

    /**
     * 查询胎面库存信息
     *
     * @param id 胎面库存信息ID
     * @return 胎面库存信息
     */
    @Override
    public TmStock selectTmStockById(Long id) {
        TmStock stock = tTmStockMapper.selectTmStockById(id);
        LambdaQueryWrapper<TmCurlRoll> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmCurlRoll::getTreadCode, stock.getMaterialCode());
        wrapper.eq(TmCurlRoll::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        TmCurlRoll curlRoll = tmCurlRollMapper.selectOne(wrapper);
        if (curlRoll != null) {
            stock.setCurlLength(curlRoll.getCurlLength());
        }
        return stock;
    }

    /**
     * 查询胎面库存信息列表
     *
     * @param tTmStock 胎面库存信息
     * @return 胎面库存信息
     */
    @Override
    public List<TmStock> selectTmStockList(TmStock tTmStock) {
        if (StringUtils.isNotEmpty(tTmStock.getEndTime())) {
            tTmStock.setEndTime(tTmStock.getEndTime() + " 23:59:59");
        }
        List<TmStock> stockList = tTmStockMapper.selectTmStockList(tTmStock);
        if (CollectionUtils.isNotEmpty(stockList)) {
            List<String> codeList = stockList.stream().map(TmStock::getMaterialCode).distinct().collect(Collectors.toList());
            Map<String, BigDecimal> lengthMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(codeList)) {
                LambdaQueryWrapper<TmCurlRoll> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(TmCurlRoll::getTreadCode, codeList);
                wrapper.eq(TmCurlRoll::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
                List<TmCurlRoll> curlRollList = tmCurlRollMapper.selectList(wrapper);
                lengthMap = curlRollList.stream().collect(Collectors.toMap(TmCurlRoll::getTreadCode, TmCurlRoll::getCurlLength));
            }
            for (TmStock stock : stockList) {
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
     * 新增胎面库存信息
     *
     * @param tTmStock 胎面库存信息
     * @return 结果
     */
    @Override
    public int insertTmStock(TmStock tTmStock) {
        tTmStock.setBaseVale(null);
        return tTmStockMapper.insertTmStock(tTmStock);
    }

    /**
     * 修改胎面库存信息
     *
     * @param tTmStock 胎面库存信息
     * @return 结果
     */
    @Override
    public int updateTmStock(TmStock tTmStock) {
        tTmStock.setBaseVale(tTmStock.getId());
        return tTmStockMapper.updateTmStock(tTmStock);
    }

    /**
     * 批量删除胎面库存信息
     *
     * @param ids 需要删除的胎面库存信息ID
     * @return 结果
     */
    @Override
    public int deleteTmStockByIds(Long[] ids) {
        return tTmStockMapper.deleteTmStockByIds(ids);
    }

    /**
     * 删除胎面库存信息信息
     *
     * @param id 胎面库存信息ID
     * @return 结果
     */
    @Override
    public int deleteTmStockById(Long id) {
        return tTmStockMapper.deleteTmStockById(id);
    }

    /**
     * 校验胎面库存唯一性（根据库存日期+物料编号+id）
     */
    public List<TmStock> checkTmStockListUnic(TmStock tTmStock) {
        return tTmStockMapper.checkTmStockListUnic(tTmStock);
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<TmStock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TmStock> importList = new ArrayList<>();

        LambdaUpdateWrapper<TmParams> paramsWrapper = new LambdaUpdateWrapper<>();
        paramsWrapper.eq(TmParams::getParamCode, "STANDARD_CRIMP_LENGTH");
        TmParams params = paramsMapper.selectOne(paramsWrapper);

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getStockDate()+a.getMaterialCode()), Collectors.counting()));

        Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
        List<TmCurlRoll> curlRollList = new ArrayList<>();
        List<String> codeList = list.stream().map(TmStock::getMaterialCode).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
        List<List<String>> splitList = CollectionUtil.splitList(codeList, 100);
        for (List<String> stringList : splitList) {
            LambdaUpdateWrapper<TmCurlRoll> wrapper = new LambdaUpdateWrapper<>();
            wrapper.in(TmCurlRoll::getTreadCode, stringList);
            wrapper.eq(TmCurlRoll::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
            curlRollList.addAll(tmCurlRollMapper.selectList(wrapper));
        }
        if (CollectionUtils.isNotEmpty(curlRollList)) {
            curlRollMap = curlRollList.stream().collect(Collectors.toMap(TmCurlRoll::getTreadCode, TmCurlRoll::getCurlLength, (m1, m2) -> m1));
        }

        for (int i = 0; i < list.size(); i++) {
            TmStock stock = list.get(i);

            String materialCode = stock.getMaterialCode();
            //重复记录校验
            Long hasValue = groupMap.get(stock.getStockDate()+materialCode);
            if (hasValue > 1) {
                failureNum++;
                stock.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.stock.stockDate");
                String columnName2 = I18nUtil.getMessage("ui.data.column.quota.treadCode");
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
                        I18nUtil.getMessage("ui.data.column.tm.stock.curlLengthNotExist"), importErrorLogs);
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
            if (CollectionUtils.isEmpty(validated)) {

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
                importList.add(stock);
            } else {
                failureNum++;
                stock.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tTmStockMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    TmStock excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    List<TmStock> unic = tTmStockMapper.checkTmStockListUnic(excelItem);
                    if (CollectionUtils.isEmpty(unic)) {
                        //不存在插入
                        successNum++;
                        tTmStockMapper.insertTmStock(excelItem);
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.stock.message.unique"), importErrorLogs);
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
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
