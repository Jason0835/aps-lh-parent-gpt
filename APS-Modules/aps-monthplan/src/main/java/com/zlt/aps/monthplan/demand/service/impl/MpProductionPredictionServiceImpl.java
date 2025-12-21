package com.zlt.aps.monthplan.demand.service.impl;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.mapper.MpProductionPredictionEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpProductionPrediction;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.common.utils.MonthCalculator;
import com.zlt.aps.monthplan.common.utils.RequirementVersionService;
import com.zlt.aps.monthplan.demand.service.IMdmProductStockService;
import com.zlt.aps.monthplan.demand.service.IMpProductionPredictionService;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.ruoyi.common.datasource.service.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.commons.collections4.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.extern.slf4j.Slf4j;

import com.zlt.common.utils.ImportExcelValidatedUtils;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpProductionPredictionServiceImpl.java
 * 描    述：MpProductionPredictionServiceImplS2-1002.未来产量预测业务层处理
 *@author yelq
 *@date 2025-12-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpProductionPredictionServiceImpl extends BaseService<MpProductionPrediction>  implements IMpProductionPredictionService
{
    private static final String PREFIX = "PRE";
    private final RequirementVersionService requirementVersionService;

    private final MpProductionPredictionEntityMapper mpProductionPredictionEntityMapper;
    private final FactoryProductionVersionMapper factoryProductionVersionMapper;
    // 销售订单
    private final ISalesOrderPoolService salesOrderPoolService;
    // 成品库存
    private final IMdmProductStockService mdmProductStockService;

    /**
     * 查询S2-1002.未来产量预测
     *
     * @param id S2-1002.未来产量预测主键
     * @return S2-1002.未来产量预测
     */
    @Override
    public MpProductionPrediction selectMpProductionPredictionById(Long id)
    {
        return mpProductionPredictionEntityMapper.selectById(id);
    }

    /**
     * 查询S2-1002.未来产量预测列表
     *
     * @param mpProductionPrediction S2-1002.未来产量预测
     * @return S2-1002.未来产量预测
     */
    @Override
    public List<MpProductionPrediction> selectMpProductionPredictionList(MpProductionPrediction mpProductionPrediction)
    {
        return mpProductionPredictionEntityMapper.selectList(null);
    }

    /**
     * 批量查询S2-1002.未来产量预测列表
     *
     * @param ids 需要查询的数据主键集合
     * @return S2-1002.未来产量预测集合
     */
    @Override
    public List<MpProductionPrediction> selectMpProductionPredictionByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    mpProductionPredictionEntityMapper::selectBatchIds
                    ,ids
        );
    }


    /**
     * 新增S2-1002.未来产量预测
     *
     * @param mpProductionPrediction S2-1002.未来产量预测
     * @return 结果
     */
    @Override
    public int insertMpProductionPrediction(MpProductionPrediction mpProductionPrediction)
    {
        mpProductionPrediction.setBaseVale(null);
        return mpProductionPredictionEntityMapper.insert(mpProductionPrediction);
    }

    /**
     * 修改S2-1002.未来产量预测
     *
     * @param mpProductionPrediction S2-1002.未来产量预测
     * @return 结果
     */
    @Override
    public int updateMpProductionPrediction(MpProductionPrediction mpProductionPrediction)
    {
        mpProductionPrediction.setBaseVale(mpProductionPrediction.getId());
        return mpProductionPredictionEntityMapper.updateById(mpProductionPrediction);
    }

    /**
     * 批量删除S2-1002.未来产量预测
     *
     * @param ids 需要删除的S2-1002.未来产量预测主键
     * @return 结果
     */
    @Override
    public int deleteMpProductionPredictionByIds(Long[] ids)
    {
        return mpProductionPredictionEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 批量删除S2-1002.未来产量预测
     *
     * @param ids 需要删除的S2-1002.未来产量预测主键
     * @return 结果
     */
    @Override
    public int deleteMpProductionPredictionByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMpProductionPredictionByIds(arrayids);
    }

    /**
     * 删除S2-1002.未来产量预测信息
     *
     * @param id S2-1002.未来产量预测主键
     * @return 结果
     */
    @Override
    public int deleteMpProductionPredictionById(Long id)
    {
        return mpProductionPredictionEntityMapper.deleteById(id);
    }

    @Override
    public void insertBatchData(Collection<MpProductionPrediction> dataList) {

       // this.mpProductionPredictionEntityMapper.insertBatchData(dataList, MpProductionPredictionEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpProductionPrediction> dataList) {

        //this.updateBatchData(dataList, MpProductionPredictionEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpProductionPrediction> list) {
        //this.mergerIntoBatchData(list, MpProductionPredictionEntityMapper.class);
    }

    /**
     * 校验S2-1002.未来产量预测唯一性
     */
    @Override
    public String checkMpProductionPredictionUnique(MpProductionPrediction mpProductionPrediction) {
       /* if (mpProductionPrediction == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpProductionPrediction> list = mpProductionPredictionEntityMapper.selectMpProductionPredictionList(mpProductionPrediction);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mpProductionPrediction.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }*/
        return UserConstants.UNIQUE;
    }
    /**
     * 导入S2-1002.未来产量预测数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MpProductionPrediction> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MpProductionPrediction> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpProductionPrediction mpProductionPrediction = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mpProductionPrediction);
            ImportExcelValidatedUtils.validatedRepeat(list,mpProductionPrediction,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mpProductionPrediction.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mpProductionPrediction.setBaseVale(null);
                importList.add(mpProductionPrediction);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                   // mpProductionPredictionEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MpProductionPrediction mpProductionPrediction = list.get(i);
                    // 错误记录跳过
                    if (mpProductionPrediction.getId() != null && mpProductionPrediction.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMpProductionPredictionUnique(mpProductionPrediction);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMpProductionPrediction(mpProductionPrediction);
                    } else {
                        failureNum++;
                        //TODO:此处需手动填写唯一校验失败国际化信息
                        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.REPEAT.getCode(),i + 2,
                                String.format(uniqueMsg, i + 2), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public AjaxResult createMonthPrediction(MpProductionPrediction createCondition) {
        // 2、得到T月、T+1月、T+2月。T月 = 当前操作日所在年月(当月) +1 ；T+1月 = 在T月的基础上+1个月；T+2月 = 在T月的基础上+2个月
        MonthCalculator.MonthRangeResult monthRangeResult = MonthCalculator.calculateMonthRanges();
        // 3、检查是否已有T月月度计划(定稿)
        //   (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
        validateProductionVersionFinalized(monthRangeResult.getTMonth());
        // 4、生成预测版本号(PRE+yyyymmdd+3位流水号)
        String predictionVersion = requirementVersionService.generateVersion(PREFIX);
        // 5、查询截止预测日，在销售订单池中的所有订单；
        List<SalesOrderPool> salesOrders = salesOrderPoolService.findCurrentSalesOrderPool();
        // 6、从成品库存表中获取库存；同时，获取T-1月新的月底计划余量(如果库存日期 > T-1月，则月底计划余量 = 0)；

        return null;
    }

    /**
     *   3、检查是否已有T月月度计划(定稿)
     *       (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
     * @param tMonth T月
     */
    private void validateProductionVersionFinalized(YearMonth tMonth) {
        Long count = factoryProductionVersionMapper.selectCount(
            Wrappers.<FactoryProductionVersion>lambdaQuery()
                .eq(FactoryProductionVersion::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE)
                .eq(FactoryProductionVersion::getYear, tMonth.getYear())
                .eq(FactoryProductionVersion::getMonth, tMonth.getMonthValue())
                .eq(FactoryProductionVersion::getIsFinal, Constant.TRUE)
        );
        if (count == 0) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
        }
    }
}
