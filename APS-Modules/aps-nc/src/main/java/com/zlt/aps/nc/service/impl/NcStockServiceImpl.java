package com.zlt.aps.nc.service.impl;

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
import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;
import com.zlt.aps.nc.api.domain.entity.NcStock;
import com.zlt.aps.nc.entity.NcParams;
import com.zlt.aps.nc.mapper.NcCurlRollMapper;
import com.zlt.aps.nc.mapper.NcParamsMapper;
import com.zlt.aps.nc.mapper.NcStockMapper;
import com.zlt.aps.nc.service.NcStockService;
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
 * 内衬库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-31
 */
@Service
public class NcStockServiceImpl implements NcStockService {
    @Autowired
    private NcStockMapper stockMapper;

    @Autowired
    private NcCurlRollMapper curlRollMapper;

    @Autowired
    private NcParamsMapper paramsMapper;

    /**
     * 查询内衬库存信息
     *
     * @param id 内衬库存信息ID
     * @return 内衬库存信息
     */
    @Override
    public NcStock selectStockById(Long id) {
        NcStock stock = stockMapper.selectStockById(id);
        LambdaQueryWrapper<NcCurlRoll> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NcCurlRoll::getLiningCode, stock.getMaterialCode());
        wrapper.eq(NcCurlRoll::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        NcCurlRoll curlRoll = curlRollMapper.selectOne(wrapper);
        if (curlRoll != null) {
            stock.setCurlLength(curlRoll.getCurlLength());
        }
        return stock;
    }

    /**
     * 查询内衬库存信息列表
     *
     * @param NcStock 内衬库存信息
     * @return 内衬库存信息
     */
    @Override
    public List<NcStock> selectStockList(NcStock stock) {
        if (StringUtils.isNotEmpty(stock.getEndTime())) {
            stock.setEndTime(stock.getEndTime() + " 23:59:59");
        }
        List<NcStock> stockList = stockMapper.selectStockList(stock);
        if (CollectionUtils.isNotEmpty(stockList)) {
            List<String> codeList = stockList.stream().map(NcStock::getMaterialCode).distinct().collect(Collectors.toList());
            Map<String, BigDecimal> lengthMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(codeList)) {
                LambdaQueryWrapper<NcCurlRoll> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(NcCurlRoll::getLiningCode, codeList);
                wrapper.eq(NcCurlRoll::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
                List<NcCurlRoll> curlRollList = curlRollMapper.selectList(wrapper);
                lengthMap = curlRollList.stream().collect(Collectors.toMap(NcCurlRoll::getLiningCode, NcCurlRoll::getCurlLength));
            }
            for (NcStock ncStock : stockList) {
                String materialCode = ncStock.getMaterialCode();
                if (lengthMap.containsKey(materialCode)) {
                    BigDecimal length = lengthMap.get(materialCode);
                    ncStock.setCurlLength(length);
                }
            }
        }
        return stockList;
    }

    /**
     * 新增内衬库存信息
     *
     * @param NcStock 内衬库存信息
     * @return 结果
     */
    @Override
    public int insertStock(NcStock stock) {
        stock.setBaseVale(null);
        return stockMapper.insertStock(stock);
    }

    /**
     * 修改内衬库存信息
     *
     * @param stock 内衬库存信息
     * @return 结果
     */
    @Override
    public int updateStock(NcStock stock) {
        stock.setBaseVale(stock.getId());
        return stockMapper.updateStock(stock);
    }

    /**
     * 批量删除内衬库存信息
     *
     * @param ids 需要删除的内衬库存信息ID
     * @return 结果
     */
    @Override
    public int deleteStockByIds(Long[] ids) {
        return stockMapper.deleteStockByIds(ids);
    }

    /**
     * 校验内衬库存唯一性（根据库存日期+物料编号+id）
     */
    public List<NcStock> checkStockListUnic(NcStock stock) {
        return stockMapper.checkStockListUnic(stock);
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
    public AjaxResult importData(List<NcStock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<NcStock> importList = new ArrayList<>();
        LambdaUpdateWrapper<NcParams> paramsWrapper = new LambdaUpdateWrapper<>();
        paramsWrapper.eq(NcParams::getParamCode, "STANDARD_CRIMP_LENGTH");
        NcParams params = paramsMapper.selectOne(paramsWrapper);

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getStockDate()+a.getMaterialCode()), Collectors.counting()));

        Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
        List<NcCurlRoll> curlRollList = new ArrayList<>();
        List<String> codeList = list.stream().map(NcStock::getMaterialCode).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
        List<List<String>> splitList = CollectionUtil.splitList(codeList, 100);
        for (List<String> stringList : splitList) {
            LambdaUpdateWrapper<NcCurlRoll> wrapper = new LambdaUpdateWrapper<>();
            wrapper.in(NcCurlRoll::getLiningCode, stringList);
            wrapper.eq(NcCurlRoll::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
            curlRollList.addAll(curlRollMapper.selectList(wrapper));
        }
        if (CollectionUtils.isNotEmpty(curlRollList)) {
            curlRollMap = curlRollList.stream().collect(Collectors.toMap(NcCurlRoll::getLiningCode, NcCurlRoll::getCurlLength, (m1, m2) -> m1));
        }

        for (int i = 0; i < list.size(); i++) {
            NcStock stock = list.get(i);

            //重复记录校验
            String materialCode = stock.getMaterialCode();
            Long hasValue = groupMap.get(stock.getStockDate()+ materialCode);
            if (hasValue > 1) {
                failureNum++;
                stock.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.stock.stockDate");
                String columnName2 = I18nUtil.getMessage("ui.data.column.quota.liningCode");
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
                        I18nUtil.getMessage("ui.data.column.nc.stock.curlLengthNotExist"), importErrorLogs);
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
                stockMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    NcStock excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }

                    // 唯一性校验
                    List<NcStock> unic = stockMapper.checkStockListUnic(excelItem);
                    if (CollectionUtils.isEmpty(unic)) {
                        //不存在插入
                        successNum++;
                        stockMapper.insertStock(excelItem);
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
