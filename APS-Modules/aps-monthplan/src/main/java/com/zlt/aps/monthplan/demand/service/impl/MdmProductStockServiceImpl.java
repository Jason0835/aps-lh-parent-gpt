package com.zlt.aps.monthplan.demand.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.demand.mapper.MdmProductStockEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMdmProductStockService;
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
 * 文件名称：MdmProductStockServiceImpl.java
 * 描    述：MdmProductStockServiceImpl成品库存业务层处理
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
public class MdmProductStockServiceImpl extends BaseService<MdmProductStock>  implements IMdmProductStockService
{
    @Autowired
    private MdmProductStockEntityMapper mdmProductStockEntityMapper;



    /**
     * 查询成品库存
     * 
     * @param id 成品库存主键
     * @return 成品库存
     */
    @Override
    public MdmProductStock selectMdmProductStockById(Long id)
    {
        return mdmProductStockEntityMapper.selectMdmProductStockById(id);
    }

    /**
     * 查询成品库存列表
     * 
     * @param mdmProductStock 成品库存
     * @return 成品库存
     */
    @Override
    public List<MdmProductStock> selectMdmProductStockList(MdmProductStock mdmProductStock)
    {
        return mdmProductStockEntityMapper.selectMdmProductStockList(mdmProductStock);
    }

    /**
     * 批量查询成品库存列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 成品库存集合
     */
    @Override
    public List<MdmProductStock> selectMdmProductStockByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    mdmProductStockEntityMapper::selectMdmProductStockByIds
                    ,ids
        );
    }


    /**
     * 新增成品库存
     * 
     * @param mdmProductStock 成品库存
     * @return 结果
     */
    @Override
    public int insertMdmProductStock(MdmProductStock mdmProductStock)
    {
        mdmProductStock.setBaseVale(null);
        return mdmProductStockEntityMapper.insert(mdmProductStock);
    }

    /**
     * 修改成品库存
     * 
     * @param mdmProductStock 成品库存
     * @return 结果
     */
    @Override
    public int updateMdmProductStock(MdmProductStock mdmProductStock)
    {
        mdmProductStock.setBaseVale(mdmProductStock.getId());
        return mdmProductStockEntityMapper.update(mdmProductStock);
    }

    /**
     * 批量删除成品库存
     * 
     * @param ids 需要删除的成品库存主键
     * @return 结果
     */
    @Override
    public int deleteMdmProductStockByIds(Long[] ids)
    {
        return mdmProductStockEntityMapper.deleteMdmProductStockByIds(ids);
    }

    /**
     * 批量删除成品库存
     *
     * @param ids 需要删除的成品库存主键
     * @return 结果
     */
    @Override
    public int deleteMdmProductStockByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMdmProductStockByIds(arrayids);
    }

    /**
     * 删除成品库存信息
     * 
     * @param id 成品库存主键
     * @return 结果
     */
    @Override
    public int deleteMdmProductStockById(Long id)
    {
        return mdmProductStockEntityMapper.deleteMdmProductStockById(id);
    }

    @Override
    public void insertBatchData(Collection<MdmProductStock> dataList) {

        this.insertBatchData(dataList, MdmProductStockEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MdmProductStock> dataList) {

        this.updateBatchData(dataList, MdmProductStockEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MdmProductStock> list) {
        this.mergerIntoBatchData(list, MdmProductStockEntityMapper.class);
    }

    /**
     * 校验成品库存唯一性
     */
    @Override
    public String checkMdmProductStockUnique(MdmProductStock mdmProductStock) {
        if (mdmProductStock == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MdmProductStock> list = mdmProductStockEntityMapper.selectMdmProductStockList(mdmProductStock);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mdmProductStock.getId())).count();
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
    public AjaxResult importData(List<MdmProductStock> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmProductStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmProductStock mdmProductStock = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mdmProductStock);
            ImportExcelValidatedUtils.validatedRepeat(list,mdmProductStock,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mdmProductStock.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mdmProductStock.setBaseVale(null);
                importList.add(mdmProductStock);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mdmProductStockEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MdmProductStock mdmProductStock = list.get(i);
                    // 错误记录跳过
                    if (mdmProductStock.getId() != null && mdmProductStock.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMdmProductStockUnique(mdmProductStock);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMdmProductStock(mdmProductStock);
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
    public List<MdmProductStock> findCurrentFinishStock() {
        MdmProductStock param = new MdmProductStock();
        param.setIsDelete(YesOrNoEnum.NO.getValue());
        return this.mdmProductStockEntityMapper.selectMdmProductStockList(param);
    }

    @Override
    public List<MdmProductStock> getMpFinishedProductStockByMaterialCode(String materialCode) {
        MdmProductStock param = new MdmProductStock();
        param.setMaterialCode(materialCode);
        param.setIsDelete(YesOrNoEnum.NO.getValue());
        return this.mdmProductStockEntityMapper.selectMdmProductStockList(param);
    }
}
