package com.zlt.aps.monthplan.demand.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.service.IMdmFinishStockService;
import com.zlt.aps.maindata.service.IMdmMonCycleSchStruConfService;
import com.zlt.aps.monthplan.api.domain.entity.MdmFinishStock;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.demand.mapper.SupplyOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
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
 * 文件名称：SupplyOrderPoolServiceImpl.java
 * 描    述：SupplyOrderPoolServiceImpl供应链订单池业务层处理
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplyOrderPoolServiceImpl extends BaseService<SupplyOrderPool>  implements ISupplyOrderPoolService
{

    private final SupplyOrderPoolEntityMapper supplyOrderPoolEntityMapper;

    private final IMdmMonCycleSchStruConfService monCycleSchStruConfService;

    private final IMdmFinishStockService finishStockService;



    /**
     * 查询供应链订单池
     * 
     * @param id 供应链订单池主键
     * @return 供应链订单池
     */
    @Override
    public SupplyOrderPool selectSupplyOrderPoolById(Long id)
    {
        return supplyOrderPoolEntityMapper.selectSupplyOrderPoolById(id);
    }

    /**
     * 查询供应链订单池列表
     * 
     * @param supplyOrderPool 供应链订单池
     * @return 供应链订单池
     */
    @Override
    public List<SupplyOrderPool> selectSupplyOrderPoolList(SupplyOrderPool supplyOrderPool)
    {
        return supplyOrderPoolEntityMapper.selectSupplyOrderPoolList(supplyOrderPool);
    }

    /**
     * 批量查询供应链订单池列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 供应链订单池集合
     */
    @Override
    public List<SupplyOrderPool> selectSupplyOrderPoolByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    supplyOrderPoolEntityMapper::selectSupplyOrderPoolByIds
                    ,ids
        );
    }


    /**
     * 新增供应链订单池
     * 
     * @param supplyOrderPool 供应链订单池
     * @return 结果
     */
    @Override
    public int insertSupplyOrderPool(SupplyOrderPool supplyOrderPool)
    {
        supplyOrderPool.setBaseVale(null);
        return supplyOrderPoolEntityMapper.insert(supplyOrderPool);
    }

    /**
     * 修改供应链订单池
     * 
     * @param supplyOrderPool 供应链订单池
     * @return 结果
     */
    @Override
    public int updateSupplyOrderPool(SupplyOrderPool supplyOrderPool)
    {
        supplyOrderPool.setBaseVale(supplyOrderPool.getId());
        return supplyOrderPoolEntityMapper.update(supplyOrderPool);
    }

    /**
     * 批量删除供应链订单池
     * 
     * @param ids 需要删除的供应链订单池主键
     * @return 结果
     */
    @Override
    public int deleteSupplyOrderPoolByIds(Long[] ids)
    {
        return supplyOrderPoolEntityMapper.deleteSupplyOrderPoolByIds(ids);
    }

    /**
     * 批量删除供应链订单池
     *
     * @param ids 需要删除的供应链订单池主键
     * @return 结果
     */
    @Override
    public int deleteSupplyOrderPoolByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteSupplyOrderPoolByIds(arrayids);
    }

    /**
     * 删除供应链订单池信息
     * 
     * @param id 供应链订单池主键
     * @return 结果
     */
    @Override
    public int deleteSupplyOrderPoolById(Long id)
    {
        return supplyOrderPoolEntityMapper.deleteSupplyOrderPoolById(id);
    }

    @Override
    public void insertBatchData(Collection<SupplyOrderPool> dataList) {

        this.insertBatchData(dataList, SupplyOrderPoolEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<SupplyOrderPool> dataList) {

        this.updateBatchData(dataList, SupplyOrderPoolEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<SupplyOrderPool> list) {
        this.mergerIntoBatchData(list, SupplyOrderPoolEntityMapper.class);
    }

    /**
     * 校验供应链订单池唯一性
     */
    @Override
    public String checkSupplyOrderPoolUnique(SupplyOrderPool supplyOrderPool) {
        if (supplyOrderPool == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<SupplyOrderPool> list = supplyOrderPoolEntityMapper.selectSupplyOrderPoolList(supplyOrderPool);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(supplyOrderPool.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入供应链订单池数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<SupplyOrderPool> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<SupplyOrderPool> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            SupplyOrderPool supplyOrderPool = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, supplyOrderPool);
            ImportExcelValidatedUtils.validatedRepeat(list,supplyOrderPool,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                supplyOrderPool.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                supplyOrderPool.setBaseVale(null);
                importList.add(supplyOrderPool);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    supplyOrderPoolEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    SupplyOrderPool supplyOrderPool = list.get(i);
                    // 错误记录跳过
                    if (supplyOrderPool.getId() != null && supplyOrderPool.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkSupplyOrderPoolUnique(supplyOrderPool);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertSupplyOrderPool(supplyOrderPool);
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
    public void createCycleStockUp(SupplyOrderPool supplyOrderPool) {
        // 1、查询当前周期性排产结构配置，如果没有周期性排产结构配置，则提示"当前没有周期性排产结构配置"；
        List<MdmMonCycleSchStruConf> cycleSchStruConfs = monCycleSchStruConfService.findCycleSchStruConf();
        if(CollectionUtils.isEmpty(cycleSchStruConfs)){
            throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleSchStruConf"));
        }
        Set<String> structures = cycleSchStruConfs.stream().map(MdmMonCycleSchStruConf::getStructureName).collect(Collectors.toSet());
        if(CollectionUtils.isEmpty(structures)){
            return;
        }
        // (1)  排除近12个月有周期性排产超期胎的SKU(超期SKU表.超期周期排产 = 1)，剩下的SKU则可生成到供应链订单池-周期排产储备
        List<MdmFinishStock> finishStocks =  finishStockService.findExcludeExceedTwelveMonth();
        if(CollectionUtils.isEmpty(finishStocks)){
            return;
        }
        List<MdmFinishStock> filterFinishStocks =  finishStocks.stream().filter(item -> structures.contains(item.getStructureName())).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(filterFinishStocks)){
            return;
        }

    }
}
