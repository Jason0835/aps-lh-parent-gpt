package com.zlt.aps.monthplan.demand.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zlt.aps.monthplan.api.domain.entity.MpDemandPlan;
import com.zlt.aps.monthplan.demand.mapper.MpDemandPlanEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpDemandPlanService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.ruoyi.common.datasource.service.BaseService;
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
 * 文件名称：MpDemandPlanServiceImpl.java
 * 描    述：MpDemandPlanServiceImpl需求计划业务层处理
 *@author yelq
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
public class MpDemandPlanServiceImpl extends BaseService<MpDemandPlan>  implements IMpDemandPlanService
{
    @Autowired
    private MpDemandPlanEntityMapper mpDemandPlanEntityMapper;

    /**
     * 查询需求计划
     * 
     * @param id 需求计划主键
     * @return 需求计划
     */
    @Override
    public MpDemandPlan selectMpDemandPlanById(Long id)
    {
        return mpDemandPlanEntityMapper.selectMpDemandPlanById(id);
    }

    /**
     * 查询需求计划列表
     * 
     * @param mpDemandPlan 需求计划
     * @return 需求计划
     */
    @Override
    public List<MpDemandPlan> selectMpDemandPlanList(MpDemandPlan mpDemandPlan)
    {
        return mpDemandPlanEntityMapper.selectMpDemandPlanList(mpDemandPlan);
    }

    /**
     * 批量查询需求计划列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 需求计划集合
     */
    @Override
    public List<MpDemandPlan> selectMpDemandPlanByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    mpDemandPlanEntityMapper::selectMpDemandPlanByIds
                    ,ids
        );
    }


    /**
     * 新增需求计划
     * 
     * @param mpDemandPlan 需求计划
     * @return 结果
     */
    @Override
    public int insertMpDemandPlan(MpDemandPlan mpDemandPlan)
    {
        mpDemandPlan.setBaseVale(null);
        return mpDemandPlanEntityMapper.insert(mpDemandPlan);
    }

    /**
     * 修改需求计划
     * 
     * @param mpDemandPlan 需求计划
     * @return 结果
     */
    @Override
    public int updateMpDemandPlan(MpDemandPlan mpDemandPlan)
    {
        mpDemandPlan.setBaseVale(mpDemandPlan.getId());
        return mpDemandPlanEntityMapper.update(mpDemandPlan);
    }

    /**
     * 批量删除需求计划
     * 
     * @param ids 需要删除的需求计划主键
     * @return 结果
     */
    @Override
    public int deleteMpDemandPlanByIds(Long[] ids)
    {
        return mpDemandPlanEntityMapper.deleteMpDemandPlanByIds(ids);
    }

    /**
     * 批量删除需求计划
     *
     * @param ids 需要删除的需求计划主键
     * @return 结果
     */
    @Override
    public int deleteMpDemandPlanByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMpDemandPlanByIds(arrayids);
    }

    /**
     * 删除需求计划信息
     * 
     * @param id 需求计划主键
     * @return 结果
     */
    @Override
    public int deleteMpDemandPlanById(Long id)
    {
        return mpDemandPlanEntityMapper.deleteMpDemandPlanById(id);
    }

    @Override
    public void insertBatchData(Collection<MpDemandPlan> dataList) {

        this.insertBatchData(dataList, MpDemandPlanEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpDemandPlan> dataList) {

        this.updateBatchData(dataList, MpDemandPlanEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpDemandPlan> list) {
        this.mergerIntoBatchData(list, MpDemandPlanEntityMapper.class);
    }

    /**
     * 校验需求计划唯一性
     */
    @Override
    public String checkMpDemandPlanUnique(MpDemandPlan mpDemandPlan) {
        if (mpDemandPlan == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpDemandPlan> list = mpDemandPlanEntityMapper.selectMpDemandPlanList(mpDemandPlan);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mpDemandPlan.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入需求计划数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MpDemandPlan> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MpDemandPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpDemandPlan mpDemandPlan = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mpDemandPlan);
            ImportExcelValidatedUtils.validatedRepeat(list,mpDemandPlan,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mpDemandPlan.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mpDemandPlan.setBaseVale(null);
                importList.add(mpDemandPlan);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mpDemandPlanEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MpDemandPlan mpDemandPlan = list.get(i);
                    // 错误记录跳过
                    if (mpDemandPlan.getId() != null && mpDemandPlan.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMpDemandPlanUnique(mpDemandPlan);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMpDemandPlan(mpDemandPlan);
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
}
