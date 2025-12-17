package com.zlt.aps.monthplan.demand.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zlt.aps.monthplan.api.domain.entity.MpOrderOffsetAllocation;
import com.zlt.aps.monthplan.demand.mapper.MpOrderOffsetAllocationEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpOrderOffsetAllocationService;
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
 * 文件名称：MpOrderOffsetAllocationServiceImpl.java
 * 描    述：MpOrderOffsetAllocationServiceImpl订单冲减分配业务层处理
 *@author yelq
 *@date 2025-12-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
public class MpOrderOffsetAllocationServiceImpl extends BaseService<MpOrderOffsetAllocation>  implements IMpOrderOffsetAllocationService
{
    @Autowired
    private MpOrderOffsetAllocationEntityMapper mpOrderOffsetAllocationEntityMapper;
    /**
     * 查询订单冲减分配
     * 
     * @param id 订单冲减分配主键
     * @return 订单冲减分配
     */
    @Override
    public MpOrderOffsetAllocation selectMpOrderOffsetAllocationById(Long id)
    {
        return mpOrderOffsetAllocationEntityMapper.selectMpOrderOffsetAllocationById(id);
    }

    /**
     * 查询订单冲减分配列表
     * 
     * @param mpOrderOffsetAllocation 订单冲减分配
     * @return 订单冲减分配
     */
    @Override
    public List<MpOrderOffsetAllocation> selectMpOrderOffsetAllocationList(MpOrderOffsetAllocation mpOrderOffsetAllocation)
    {
        return mpOrderOffsetAllocationEntityMapper.selectMpOrderOffsetAllocationList(mpOrderOffsetAllocation);
    }

    /**
     * 批量查询订单冲减分配列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 订单冲减分配集合
     */
    @Override
    public List<MpOrderOffsetAllocation> selectMpOrderOffsetAllocationByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    mpOrderOffsetAllocationEntityMapper::selectMpOrderOffsetAllocationByIds
                    ,ids
        );
    }


    /**
     * 新增订单冲减分配
     * 
     * @param mpOrderOffsetAllocation 订单冲减分配
     * @return 结果
     */
    @Override
    public int insertMpOrderOffsetAllocation(MpOrderOffsetAllocation mpOrderOffsetAllocation)
    {
        mpOrderOffsetAllocation.setBaseVale(null);
        return mpOrderOffsetAllocationEntityMapper.insert(mpOrderOffsetAllocation);
    }

    /**
     * 修改订单冲减分配
     * 
     * @param mpOrderOffsetAllocation 订单冲减分配
     * @return 结果
     */
    @Override
    public int updateMpOrderOffsetAllocation(MpOrderOffsetAllocation mpOrderOffsetAllocation)
    {
        mpOrderOffsetAllocation.setBaseVale(mpOrderOffsetAllocation.getId());
        return mpOrderOffsetAllocationEntityMapper.update(mpOrderOffsetAllocation);
    }

    /**
     * 批量删除订单冲减分配
     * 
     * @param ids 需要删除的订单冲减分配主键
     * @return 结果
     */
    @Override
    public int deleteMpOrderOffsetAllocationByIds(Long[] ids)
    {
        return mpOrderOffsetAllocationEntityMapper.deleteMpOrderOffsetAllocationByIds(ids);
    }

    /**
     * 批量删除订单冲减分配
     *
     * @param ids 需要删除的订单冲减分配主键
     * @return 结果
     */
    @Override
    public int deleteMpOrderOffsetAllocationByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMpOrderOffsetAllocationByIds(arrayids);
    }

    /**
     * 删除订单冲减分配信息
     * 
     * @param id 订单冲减分配主键
     * @return 结果
     */
    @Override
    public int deleteMpOrderOffsetAllocationById(Long id)
    {
        return mpOrderOffsetAllocationEntityMapper.deleteMpOrderOffsetAllocationById(id);
    }

    @Override
    public void insertBatchData(Collection<MpOrderOffsetAllocation> dataList) {

        this.insertBatchData(dataList, MpOrderOffsetAllocationEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpOrderOffsetAllocation> dataList) {

        this.updateBatchData(dataList, MpOrderOffsetAllocationEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpOrderOffsetAllocation> list) {
        this.mergerIntoBatchData(list, MpOrderOffsetAllocationEntityMapper.class);
    }

    /**
     * 校验订单冲减分配唯一性
     */
    @Override
    public String checkMpOrderOffsetAllocationUnique(MpOrderOffsetAllocation mpOrderOffsetAllocation) {
        if (mpOrderOffsetAllocation == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpOrderOffsetAllocation> list = mpOrderOffsetAllocationEntityMapper.selectMpOrderOffsetAllocationList(mpOrderOffsetAllocation);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mpOrderOffsetAllocation.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入订单冲减分配数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MpOrderOffsetAllocation> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MpOrderOffsetAllocation> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpOrderOffsetAllocation mpOrderOffsetAllocation = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mpOrderOffsetAllocation);
            ImportExcelValidatedUtils.validatedRepeat(list,mpOrderOffsetAllocation,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mpOrderOffsetAllocation.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mpOrderOffsetAllocation.setBaseVale(null);
                importList.add(mpOrderOffsetAllocation);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mpOrderOffsetAllocationEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MpOrderOffsetAllocation mpOrderOffsetAllocation = list.get(i);
                    // 错误记录跳过
                    if (mpOrderOffsetAllocation.getId() != null && mpOrderOffsetAllocation.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMpOrderOffsetAllocationUnique(mpOrderOffsetAllocation);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMpOrderOffsetAllocation(mpOrderOffsetAllocation);
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
