package com.zlt.aps.monthplan.demand.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.demand.mapper.DpOrderOffsetDetailEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpOrderOffsetDetailService;
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
 * 文件名称：DpOrderOffsetDetailServiceImpl.java
 * 描    述：DpOrderOffsetDetailServiceImplS1-0604订单冲减分配业务层处理
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
public class DpOrderOffsetDetailServiceImpl extends BaseService<DpOrderOffsetDetail>  implements IDpOrderOffsetDetailService
{
    @Autowired
    private DpOrderOffsetDetailEntityMapper dpOrderOffsetDetailEntityMapper;



    /**
     * 查询S1-0604订单冲减分配
     * 
     * @param id S1-0604订单冲减分配主键
     * @return S1-0604订单冲减分配
     */
    @Override
    public DpOrderOffsetDetail selectDpOrderOffsetDetailById(Long id)
    {
        return dpOrderOffsetDetailEntityMapper.selectDpOrderOffsetDetailById(id);
    }

    /**
     * 查询S1-0604订单冲减分配列表
     * 
     * @param dpOrderOffsetDetail S1-0604订单冲减分配
     * @return S1-0604订单冲减分配
     */
    @Override
    public List<DpOrderOffsetDetail> selectDpOrderOffsetDetailList(DpOrderOffsetDetail dpOrderOffsetDetail)
    {
        return dpOrderOffsetDetailEntityMapper.selectDpOrderOffsetDetailList(dpOrderOffsetDetail);
    }

    /**
     * 批量查询S1-0604订单冲减分配列表
     *
     * @param ids 需要查询的数据主键集合
     * @return S1-0604订单冲减分配集合
     */
    @Override
    public List<DpOrderOffsetDetail> selectDpOrderOffsetDetailByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    dpOrderOffsetDetailEntityMapper::selectDpOrderOffsetDetailByIds
                    ,ids
        );
    }


    /**
     * 新增S1-0604订单冲减分配
     * 
     * @param dpOrderOffsetDetail S1-0604订单冲减分配
     * @return 结果
     */
    @Override
    public int insertDpOrderOffsetDetail(DpOrderOffsetDetail dpOrderOffsetDetail)
    {
        dpOrderOffsetDetail.setBaseVale(null);
        return dpOrderOffsetDetailEntityMapper.insert(dpOrderOffsetDetail);
    }

    /**
     * 修改S1-0604订单冲减分配
     * 
     * @param dpOrderOffsetDetail S1-0604订单冲减分配
     * @return 结果
     */
    @Override
    public int updateDpOrderOffsetDetail(DpOrderOffsetDetail dpOrderOffsetDetail)
    {
        dpOrderOffsetDetail.setBaseVale(dpOrderOffsetDetail.getId());
        return dpOrderOffsetDetailEntityMapper.update(dpOrderOffsetDetail);
    }

    /**
     * 批量删除S1-0604订单冲减分配
     * 
     * @param ids 需要删除的S1-0604订单冲减分配主键
     * @return 结果
     */
    @Override
    public int deleteDpOrderOffsetDetailByIds(Long[] ids)
    {
        return dpOrderOffsetDetailEntityMapper.deleteDpOrderOffsetDetailByIds(ids);
    }

    /**
     * 批量删除S1-0604订单冲减分配
     *
     * @param ids 需要删除的S1-0604订单冲减分配主键
     * @return 结果
     */
    @Override
    public int deleteDpOrderOffsetDetailByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteDpOrderOffsetDetailByIds(arrayids);
    }

    /**
     * 删除S1-0604订单冲减分配信息
     * 
     * @param id S1-0604订单冲减分配主键
     * @return 结果
     */
    @Override
    public int deleteDpOrderOffsetDetailById(Long id)
    {
        return dpOrderOffsetDetailEntityMapper.deleteDpOrderOffsetDetailById(id);
    }

    @Override
    public void insertBatchData(Collection<DpOrderOffsetDetail> dataList) {

        this.insertBatchData(dataList, DpOrderOffsetDetailEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<DpOrderOffsetDetail> dataList) {

        this.updateBatchData(dataList, DpOrderOffsetDetailEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<DpOrderOffsetDetail> list) {
        this.mergerIntoBatchData(list, DpOrderOffsetDetailEntityMapper.class);
    }

    /**
     * 校验S1-0604订单冲减分配唯一性
     */
    @Override
    public String checkDpOrderOffsetDetailUnique(DpOrderOffsetDetail dpOrderOffsetDetail) {
        if (dpOrderOffsetDetail == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<DpOrderOffsetDetail> list = dpOrderOffsetDetailEntityMapper.selectDpOrderOffsetDetailList(dpOrderOffsetDetail);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(dpOrderOffsetDetail.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入S1-0604订单冲减分配数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<DpOrderOffsetDetail> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<DpOrderOffsetDetail> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DpOrderOffsetDetail dpOrderOffsetDetail = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, dpOrderOffsetDetail);
            ImportExcelValidatedUtils.validatedRepeat(list,dpOrderOffsetDetail,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                dpOrderOffsetDetail.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                dpOrderOffsetDetail.setBaseVale(null);
                importList.add(dpOrderOffsetDetail);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    dpOrderOffsetDetailEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    DpOrderOffsetDetail dpOrderOffsetDetail = list.get(i);
                    // 错误记录跳过
                    if (dpOrderOffsetDetail.getId() != null && dpOrderOffsetDetail.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkDpOrderOffsetDetailUnique(dpOrderOffsetDetail);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertDpOrderOffsetDetail(dpOrderOffsetDetail);
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
