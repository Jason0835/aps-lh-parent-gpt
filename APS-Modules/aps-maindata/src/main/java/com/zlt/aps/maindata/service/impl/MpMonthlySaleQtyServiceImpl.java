package com.zlt.aps.maindata.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zlt.aps.maindata.mapper.MpMonthlySaleQtyEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthlySaleQtyService;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
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
 * 文件名称：MpMonthlySaleQtyServiceImpl.java
 * 描    述：MpMonthlySaleQtyServiceImpl月均销量业务层处理
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
public class MpMonthlySaleQtyServiceImpl extends BaseService<MpMonthlySaleQty>  implements IMpMonthlySaleQtyService
{
    @Autowired
    private MpMonthlySaleQtyEntityMapper mpMonthlySaleQtyEntityMapper;



    /**
     * 查询月均销量
     * 
     * @param id 月均销量主键
     * @return 月均销量
     */
    @Override
    public MpMonthlySaleQty selectMpMonthlySaleQtyById(Long id)
    {
        return mpMonthlySaleQtyEntityMapper.selectMpMonthlySaleQtyById(id);
    }

    /**
     * 查询月均销量列表
     * 
     * @param mpMonthlySaleQty 月均销量
     * @return 月均销量
     */
    @Override
    public List<MpMonthlySaleQty> selectMpMonthlySaleQtyList(MpMonthlySaleQty mpMonthlySaleQty)
    {
        return mpMonthlySaleQtyEntityMapper.selectMpMonthlySaleQtyList(mpMonthlySaleQty);
    }

    /**
     * 批量查询月均销量列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 月均销量集合
     */
    @Override
    public List<MpMonthlySaleQty> selectMpMonthlySaleQtyByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    mpMonthlySaleQtyEntityMapper::selectMpMonthlySaleQtyByIds
                    ,ids
        );
    }


    /**
     * 新增月均销量
     * 
     * @param mpMonthlySaleQty 月均销量
     * @return 结果
     */
    @Override
    public int insertMpMonthlySaleQty(MpMonthlySaleQty mpMonthlySaleQty)
    {
        mpMonthlySaleQty.setBaseVale(null);
        return mpMonthlySaleQtyEntityMapper.insert(mpMonthlySaleQty);
    }

    /**
     * 修改月均销量
     * 
     * @param mpMonthlySaleQty 月均销量
     * @return 结果
     */
    @Override
    public int updateMpMonthlySaleQty(MpMonthlySaleQty mpMonthlySaleQty)
    {
        mpMonthlySaleQty.setBaseVale(mpMonthlySaleQty.getId());
        return mpMonthlySaleQtyEntityMapper.update(mpMonthlySaleQty);
    }

    /**
     * 批量删除月均销量
     * 
     * @param ids 需要删除的月均销量主键
     * @return 结果
     */
    @Override
    public int deleteMpMonthlySaleQtyByIds(Long[] ids)
    {
        return mpMonthlySaleQtyEntityMapper.deleteMpMonthlySaleQtyByIds(ids);
    }

    /**
     * 批量删除月均销量
     *
     * @param ids 需要删除的月均销量主键
     * @return 结果
     */
    @Override
    public int deleteMpMonthlySaleQtyByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMpMonthlySaleQtyByIds(arrayids);
    }

    /**
     * 删除月均销量信息
     * 
     * @param id 月均销量主键
     * @return 结果
     */
    @Override
    public int deleteMpMonthlySaleQtyById(Long id)
    {
        return mpMonthlySaleQtyEntityMapper.deleteMpMonthlySaleQtyById(id);
    }

    @Override
    public void insertBatchData(Collection<MpMonthlySaleQty> dataList) {

        this.insertBatchData(dataList, MpMonthlySaleQtyEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpMonthlySaleQty> dataList) {

        this.updateBatchData(dataList, MpMonthlySaleQtyEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpMonthlySaleQty> list) {
        this.mergerIntoBatchData(list, MpMonthlySaleQtyEntityMapper.class);
    }

    /**
     * 校验月均销量唯一性
     */
    @Override
    public String checkMpMonthlySaleQtyUnique(MpMonthlySaleQty mpMonthlySaleQty) {
        if (mpMonthlySaleQty == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpMonthlySaleQty> list = mpMonthlySaleQtyEntityMapper.selectMpMonthlySaleQtyList(mpMonthlySaleQty);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mpMonthlySaleQty.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入月均销量数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MpMonthlySaleQty> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MpMonthlySaleQty> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpMonthlySaleQty mpMonthlySaleQty = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mpMonthlySaleQty);
            ImportExcelValidatedUtils.validatedRepeat(list,mpMonthlySaleQty,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mpMonthlySaleQty.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mpMonthlySaleQty.setBaseVale(null);
                importList.add(mpMonthlySaleQty);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mpMonthlySaleQtyEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MpMonthlySaleQty mpMonthlySaleQty = list.get(i);
                    // 错误记录跳过
                    if (mpMonthlySaleQty.getId() != null && mpMonthlySaleQty.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMpMonthlySaleQtyUnique(mpMonthlySaleQty);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMpMonthlySaleQty(mpMonthlySaleQty);
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
