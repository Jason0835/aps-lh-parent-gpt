package com.zlt.aps.maindata.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zlt.aps.maindata.mapper.MpHistorySaleRecordEntityMapper;
import com.zlt.aps.maindata.service.IMpHistorySaleRecordService;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
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
 * 文件名称：MpHistorySaleRecordServiceImpl.java
 * 描    述：MpHistorySaleRecordServiceImpl历史销售记录业务层处理
 *@author yelq
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
public class MpHistorySaleRecordServiceImpl extends BaseService<MpHistorySaleRecord>  implements IMpHistorySaleRecordService
{
    @Autowired
    private MpHistorySaleRecordEntityMapper mpHistorySaleRecordEntityMapper;


    /**
     * 查询历史销售记录
     * 
     * @param id 历史销售记录主键
     * @return 历史销售记录
     */
    @Override
    public MpHistorySaleRecord selectMpHistorySaleRecordById(Long id)
    {
        return mpHistorySaleRecordEntityMapper.selectMpHistorySaleRecordById(id);
    }

    /**
     * 查询历史销售记录列表
     * 
     * @param mpHistorySaleRecord 历史销售记录
     * @return 历史销售记录
     */
    @Override
    public List<MpHistorySaleRecord> selectMpHistorySaleRecordList(MpHistorySaleRecord mpHistorySaleRecord)
    {
        return mpHistorySaleRecordEntityMapper.selectMpHistorySaleRecordList(mpHistorySaleRecord);
    }

    /**
     * 批量查询历史销售记录列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 历史销售记录集合
     */
    @Override
    public List<MpHistorySaleRecord> selectMpHistorySaleRecordByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    mpHistorySaleRecordEntityMapper::selectMpHistorySaleRecordByIds
                    ,ids
        );
    }


    /**
     * 新增历史销售记录
     * 
     * @param mpHistorySaleRecord 历史销售记录
     * @return 结果
     */
    @Override
    public int insertMpHistorySaleRecord(MpHistorySaleRecord mpHistorySaleRecord)
    {
        mpHistorySaleRecord.setBaseVale(null);
        return mpHistorySaleRecordEntityMapper.insert(mpHistorySaleRecord);
    }

    /**
     * 修改历史销售记录
     * 
     * @param mpHistorySaleRecord 历史销售记录
     * @return 结果
     */
    @Override
    public int updateMpHistorySaleRecord(MpHistorySaleRecord mpHistorySaleRecord)
    {
        mpHistorySaleRecord.setBaseVale(mpHistorySaleRecord.getId());
        return mpHistorySaleRecordEntityMapper.update(mpHistorySaleRecord);
    }

    /**
     * 批量删除历史销售记录
     * 
     * @param ids 需要删除的历史销售记录主键
     * @return 结果
     */
    @Override
    public int deleteMpHistorySaleRecordByIds(Long[] ids)
    {
        return mpHistorySaleRecordEntityMapper.deleteMpHistorySaleRecordByIds(ids);
    }

    /**
     * 批量删除历史销售记录
     *
     * @param ids 需要删除的历史销售记录主键
     * @return 结果
     */
    @Override
    public int deleteMpHistorySaleRecordByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMpHistorySaleRecordByIds(arrayids);
    }

    /**
     * 删除历史销售记录信息
     * 
     * @param id 历史销售记录主键
     * @return 结果
     */
    @Override
    public int deleteMpHistorySaleRecordById(Long id)
    {
        return mpHistorySaleRecordEntityMapper.deleteMpHistorySaleRecordById(id);
    }

    @Override
    public void insertBatchData(Collection<MpHistorySaleRecord> dataList) {

        this.insertBatchData(dataList, MpHistorySaleRecordEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpHistorySaleRecord> dataList) {

        this.updateBatchData(dataList, MpHistorySaleRecordEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpHistorySaleRecord> list) {
        this.mergerIntoBatchData(list, MpHistorySaleRecordEntityMapper.class);
    }

    /**
     * 校验历史销售记录唯一性
     */
    @Override
    public String checkMpHistorySaleRecordUnique(MpHistorySaleRecord mpHistorySaleRecord) {
        if (mpHistorySaleRecord == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpHistorySaleRecord> list = mpHistorySaleRecordEntityMapper.selectMpHistorySaleRecordList(mpHistorySaleRecord);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mpHistorySaleRecord.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入历史销售记录数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MpHistorySaleRecord> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MpHistorySaleRecord> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpHistorySaleRecord mpHistorySaleRecord = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mpHistorySaleRecord);
            ImportExcelValidatedUtils.validatedRepeat(list,mpHistorySaleRecord,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mpHistorySaleRecord.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mpHistorySaleRecord.setBaseVale(null);
                importList.add(mpHistorySaleRecord);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mpHistorySaleRecordEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MpHistorySaleRecord mpHistorySaleRecord = list.get(i);
                    // 错误记录跳过
                    if (mpHistorySaleRecord.getId() != null && mpHistorySaleRecord.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMpHistorySaleRecordUnique(mpHistorySaleRecord);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMpHistorySaleRecord(mpHistorySaleRecord);
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
