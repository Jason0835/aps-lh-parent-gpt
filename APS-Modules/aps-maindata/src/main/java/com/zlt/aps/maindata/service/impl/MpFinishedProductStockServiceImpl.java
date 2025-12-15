package com.zlt.aps.maindata.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MpFinishedProductStockEntityMapper;
import com.zlt.aps.maindata.service.IMpFinishedProductStockService;
import com.zlt.aps.monthplan.api.domain.entity.MpFinishedProductStock;
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
 * 文件名称：MpFinishedProductStockServiceImpl.java
 * 描    述：MpFinishedProductStockServiceImpl成品库存业务层处理
 *@author yelq
 *@date 2025-12-15
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
public class MpFinishedProductStockServiceImpl extends BaseService<MpFinishedProductStock>  implements IMpFinishedProductStockService
{
    @Autowired
    private MpFinishedProductStockEntityMapper mpFinishedProductStockEntityMapper;



    /**
     * 查询成品库存
     * 
     * @param id 成品库存主键
     * @return 成品库存
     */
    @Override
    public MpFinishedProductStock selectMpFinishedProductStockById(Long id)
    {
        return mpFinishedProductStockEntityMapper.selectMpFinishedProductStockById(id);
    }

    /**
     * 查询成品库存列表
     * 
     * @param mpFinishedProductStock 成品库存
     * @return 成品库存
     */
    @Override
    public List<MpFinishedProductStock> selectMpFinishedProductStockList(MpFinishedProductStock mpFinishedProductStock)
    {
        return mpFinishedProductStockEntityMapper.selectMpFinishedProductStockList(mpFinishedProductStock);
    }

    /**
     * 批量查询成品库存列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 成品库存集合
     */
    @Override
    public List<MpFinishedProductStock> selectMpFinishedProductStockByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    mpFinishedProductStockEntityMapper::selectMpFinishedProductStockByIds
                    ,ids
        );
    }


    /**
     * 新增成品库存
     * 
     * @param mpFinishedProductStock 成品库存
     * @return 结果
     */
    @Override
    public int insertMpFinishedProductStock(MpFinishedProductStock mpFinishedProductStock)
    {
        mpFinishedProductStock.setBaseVale(null);
        return mpFinishedProductStockEntityMapper.insert(mpFinishedProductStock);
    }

    /**
     * 修改成品库存
     * 
     * @param mpFinishedProductStock 成品库存
     * @return 结果
     */
    @Override
    public int updateMpFinishedProductStock(MpFinishedProductStock mpFinishedProductStock)
    {
        mpFinishedProductStock.setBaseVale(mpFinishedProductStock.getId());
        return mpFinishedProductStockEntityMapper.update(mpFinishedProductStock);
    }

    /**
     * 批量删除成品库存
     * 
     * @param ids 需要删除的成品库存主键
     * @return 结果
     */
    @Override
    public int deleteMpFinishedProductStockByIds(Long[] ids)
    {
        return mpFinishedProductStockEntityMapper.deleteMpFinishedProductStockByIds(ids);
    }

    /**
     * 批量删除成品库存
     *
     * @param ids 需要删除的成品库存主键
     * @return 结果
     */
    @Override
    public int deleteMpFinishedProductStockByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMpFinishedProductStockByIds(arrayids);
    }

    /**
     * 删除成品库存信息
     * 
     * @param id 成品库存主键
     * @return 结果
     */
    @Override
    public int deleteMpFinishedProductStockById(Long id)
    {
        return mpFinishedProductStockEntityMapper.deleteMpFinishedProductStockById(id);
    }

    @Override
    public void insertBatchData(Collection<MpFinishedProductStock> dataList) {

        this.insertBatchData(dataList, MpFinishedProductStockEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpFinishedProductStock> dataList) {

        this.updateBatchData(dataList, MpFinishedProductStockEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpFinishedProductStock> list) {
        this.mergerIntoBatchData(list, MpFinishedProductStockEntityMapper.class);
    }

    /**
     * 校验成品库存唯一性
     */
    @Override
    public String checkMpFinishedProductStockUnique(MpFinishedProductStock mpFinishedProductStock) {
        if (mpFinishedProductStock == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpFinishedProductStock> list = mpFinishedProductStockEntityMapper.selectMpFinishedProductStockList(mpFinishedProductStock);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mpFinishedProductStock.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入成品库存数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MpFinishedProductStock> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MpFinishedProductStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpFinishedProductStock mpFinishedProductStock = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mpFinishedProductStock);
            ImportExcelValidatedUtils.validatedRepeat(list,mpFinishedProductStock,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mpFinishedProductStock.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mpFinishedProductStock.setBaseVale(null);
                importList.add(mpFinishedProductStock);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mpFinishedProductStockEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MpFinishedProductStock mpFinishedProductStock = list.get(i);
                    // 错误记录跳过
                    if (mpFinishedProductStock.getId() != null && mpFinishedProductStock.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMpFinishedProductStockUnique(mpFinishedProductStock);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMpFinishedProductStock(mpFinishedProductStock);
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
    public List<MpFinishedProductStock> findCurrentFinishStock() {
        MpFinishedProductStock param = new MpFinishedProductStock();
        param.setIsDelete(YesOrNoEnum.NO.getValue());
        return this.mpFinishedProductStockEntityMapper.selectMpFinishedProductStockList(param);
    }

    @Override
    public MpFinishedProductStock getMpFinishedProductStockByMaterialCode(String materialCode) {
        MpFinishedProductStock param = new MpFinishedProductStock();
        param.setMaterialCode(materialCode);
        param.setIsDelete(YesOrNoEnum.NO.getValue());
        List<MpFinishedProductStock> finishedProductStocks =  this.mpFinishedProductStockEntityMapper.selectMpFinishedProductStockList(param);
        return CollectionUtils.isNotEmpty(finishedProductStocks) ? finishedProductStocks.get(0) : null;
    }
}
