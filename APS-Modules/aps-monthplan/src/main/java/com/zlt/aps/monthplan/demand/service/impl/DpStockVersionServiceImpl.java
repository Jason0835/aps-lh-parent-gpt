package com.zlt.aps.monthplan.demand.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpStockVersion;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpProductionPrediction;
import com.zlt.aps.monthplan.demand.mapper.DpStockVersionEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpStockVersionService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.ruoyi.common.datasource.service.BaseService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 文件名称：DpStockVersionServiceImpl.java
 * 描    述：DpStockVersionServiceImpl需求计划_版本库存业务层处理
 *@author yelq
 *@date 2025-12-20
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
public class DpStockVersionServiceImpl extends BaseService<DpStockVersion>  implements IDpStockVersionService
{
    @Autowired
    private DpStockVersionEntityMapper dpStockVersionEntityMapper;



    /**
     * 查询需求计划_版本库存
     * 
     * @param id 需求计划_版本库存主键
     * @return 需求计划_版本库存
     */
    @Override
    public DpStockVersion selectDpStockVersionById(Long id)
    {
        return dpStockVersionEntityMapper.selectDpStockVersionById(id);
    }

    /**
     * 查询需求计划_版本库存列表
     * 
     * @param dpStockVersion 需求计划_版本库存
     * @return 需求计划_版本库存
     */
    @Override
    public List<DpStockVersion> selectDpStockVersionList(DpStockVersion dpStockVersion)
    {
        return dpStockVersionEntityMapper.selectDpStockVersionList(dpStockVersion);
    }

    /**
     * 批量查询需求计划_版本库存列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 需求计划_版本库存集合
     */
    @Override
    public List<DpStockVersion> selectDpStockVersionByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    dpStockVersionEntityMapper::selectDpStockVersionByIds
                    ,ids
        );
    }


    /**
     * 新增需求计划_版本库存
     * 
     * @param dpStockVersion 需求计划_版本库存
     * @return 结果
     */
    @Override
    public int insertDpStockVersion(DpStockVersion dpStockVersion)
    {
        dpStockVersion.setBaseVale(null);
        return dpStockVersionEntityMapper.insert(dpStockVersion);
    }

    /**
     * 修改需求计划_版本库存
     * 
     * @param dpStockVersion 需求计划_版本库存
     * @return 结果
     */
    @Override
    public int updateDpStockVersion(DpStockVersion dpStockVersion)
    {
        dpStockVersion.setBaseVale(dpStockVersion.getId());
        return dpStockVersionEntityMapper.update(dpStockVersion);
    }

    /**
     * 批量删除需求计划_版本库存
     * 
     * @param ids 需要删除的需求计划_版本库存主键
     * @return 结果
     */
    @Override
    public int deleteDpStockVersionByIds(Long[] ids)
    {
        return dpStockVersionEntityMapper.deleteDpStockVersionByIds(ids);
    }

    /**
     * 批量删除需求计划_版本库存
     *
     * @param ids 需要删除的需求计划_版本库存主键
     * @return 结果
     */
    @Override
    public int deleteDpStockVersionByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteDpStockVersionByIds(arrayids);
    }

    /**
     * 删除需求计划_版本库存信息
     * 
     * @param id 需求计划_版本库存主键
     * @return 结果
     */
    @Override
    public int deleteDpStockVersionById(Long id)
    {
        return dpStockVersionEntityMapper.deleteDpStockVersionById(id);
    }

    @Override
    public void insertBatchData(Collection<DpStockVersion> dataList) {

        this.insertBatchData(dataList, DpStockVersionEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<DpStockVersion> dataList) {

        this.updateBatchData(dataList, DpStockVersionEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<DpStockVersion> list) {
        this.mergerIntoBatchData(list, DpStockVersionEntityMapper.class);
    }

    /**
     * 校验需求计划_版本库存唯一性
     */
    @Override
    public String checkDpStockVersionUnique(DpStockVersion dpStockVersion) {
        if (dpStockVersion == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<DpStockVersion> list = dpStockVersionEntityMapper.selectDpStockVersionList(dpStockVersion);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(dpStockVersion.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入需求计划_版本库存数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<DpStockVersion> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<DpStockVersion> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DpStockVersion dpStockVersion = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, dpStockVersion);
            ImportExcelValidatedUtils.validatedRepeat(list,dpStockVersion,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                dpStockVersion.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                dpStockVersion.setBaseVale(null);
                importList.add(dpStockVersion);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    dpStockVersionEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    DpStockVersion dpStockVersion = list.get(i);
                    // 错误记录跳过
                    if (dpStockVersion.getId() != null && dpStockVersion.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkDpStockVersionUnique(dpStockVersion);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertDpStockVersion(dpStockVersion);
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
    public void insertBatchData(DpDemandPlan createCondition, String monthPlanVersion, Map<String, List<MdmProductStock>> finishedProductStockMap) {
        if (org.springframework.util.CollectionUtils.isEmpty(finishedProductStockMap)) {
            return;
        }
        List<DpStockVersion> list = Lists.newArrayList();
        List<MdmProductStock> finishedProductStocks = flattenStockMap(finishedProductStockMap);
        finishedProductStocks.forEach(finishedProductStock -> {
            DpStockVersion requireStock = this.buildRequireStock(createCondition,monthPlanVersion,finishedProductStock);
            list.add(requireStock);
        });
        this.insertBatchData(list);
    }

    @Override
    public void insertBatchData(MpProductionPrediction createCondition, Map<String, List<MdmProductStock>> finishedProductStockMap) {
        if (org.springframework.util.CollectionUtils.isEmpty(finishedProductStockMap)) {
            return;
        }
        List<DpStockVersion> list = Lists.newArrayList();
        List<MdmProductStock> finishedProductStocks = flattenStockMap(finishedProductStockMap);
        finishedProductStocks.forEach(finishedProductStock -> {
            DpStockVersion requireStock = this.buildRequireStock(createCondition,finishedProductStock);
            list.add(requireStock);
        });
        this.insertBatchData(list);
    }

    private DpStockVersion buildRequireStock(MpProductionPrediction createCondition, MdmProductStock finishedProductStock) {
        DpStockVersion requireStock = new DpStockVersion();
        BeanUtils.copyProperties(finishedProductStock, requireStock);
        requireStock.setId(null);
        requireStock.setRequireVersion(createCondition.getPredictionVersion());
        requireStock.setIsDelete(YesOrNoEnum.NO.getValue());
        requireStock.setRemainingQty(finishedProductStock.getLeftOverQty());
        requireStock.setBaseVale(null);
        requireStock.setYear(createCondition.getYear());
        requireStock.setMonth(createCondition.getMonth());
        return requireStock;
    }

    private DpStockVersion buildRequireStock(DpDemandPlan createCondition, String monthPlanVersion, MdmProductStock finishedProductStock) {
        DpStockVersion requireStock = new DpStockVersion();
        BeanUtils.copyProperties(finishedProductStock, requireStock);
        requireStock.setId(null);
        requireStock.setRequireVersion(monthPlanVersion);
        requireStock.setIsDelete(YesOrNoEnum.NO.getValue());
        requireStock.setRemainingQty(finishedProductStock.getLeftOverQty());
        requireStock.setBaseVale(null);
        requireStock.setYear(createCondition.getYear());
        requireStock.setMonth(createCondition.getMonth());
        return requireStock;
    }

    /**
     * 将Map转换为List<MpFinishedProductStock>
     */
    public List<MdmProductStock> flattenStockMap(
        Map<String, List<MdmProductStock>> finishedProductStockMap) {
        // 使用Stream扁平化转换
        return finishedProductStockMap.values().stream()
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
