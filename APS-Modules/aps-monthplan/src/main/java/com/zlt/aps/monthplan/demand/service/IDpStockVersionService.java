package com.zlt.aps.monthplan.demand.service;

import java.util.List;
import java.util.Map;

import com.ruoyi.common.datasource.service.IBaseService;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpStockVersion;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpProductionPrediction;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpStockVersionService.java
 * 描    述：IDpStockVersionService需求计划_版本库存后端接口
 *@author yelq
 *@date 2025-12-20
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IDpStockVersionService  extends IBaseService<DpStockVersion>
{
    /**
     * 查询需求计划_版本库存
     * 
     * @param id 需求计划_版本库存主键
     * @return 需求计划_版本库存
     */
    DpStockVersion selectDpStockVersionById(Long id);

    /**
     * 查询需求计划_版本库存列表
     * 
     * @param dpStockVersion 需求计划_版本库存
     * @return 需求计划_版本库存集合
     */
    List<DpStockVersion> selectDpStockVersionList(DpStockVersion dpStockVersion);

    /**
     * 批量查询需求计划_版本库存列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 需求计划_版本库存集合
     */
    List<DpStockVersion> selectDpStockVersionByIds(List<Long> ids);


    /**
     * 新增需求计划_版本库存
     * 
     * @param dpStockVersion 需求计划_版本库存
     * @return 结果
     */
    @Transactional
    int insertDpStockVersion(DpStockVersion dpStockVersion);

    /**
     * 修改需求计划_版本库存
     * 
     * @param dpStockVersion 需求计划_版本库存
     * @return 结果
     */
    @Transactional
    int updateDpStockVersion(DpStockVersion dpStockVersion);

    /**
     * 批量删除需求计划_版本库存
     * 
     * @param ids 需要删除的需求计划_版本库存主键集合
     * @return 结果
     */
   
    @Transactional
    int deleteDpStockVersionByIds(Long[] ids);

    /**
     * 批量删除需求计划_版本库存
     *
     * @param ids 需要删除的需求计划_版本库存主键集合
     * @return 结果
     */

    @Transactional
    int deleteDpStockVersionByIds(List<Long> ids);

    /**
     * 删除需求计划_版本库存信息
     * 
     * @param id 需求计划_版本库存主键
     * @return 结果
     */
    @Transactional
    int deleteDpStockVersionById(Long id);

    /**
     * 校验需求计划_版本库存唯一性
     */
    String checkDpStockVersionUnique(DpStockVersion dpStockVersion);

    /**
     * 导入需求计划_版本库存数据
     */
    @Transactional
    AjaxResult importData(List<DpStockVersion> list, boolean updateSupport, Long importLogId);
    /**
     * 将分配时的成品库存记录到库存版本表中(以需求版本号的维度)；
     * @param createCondition 需求参数
     * @param monthPlanVersion 需求版本号
     * @param finishedProductStockMap 成品库存记录
     */
    void insertBatchData(DpDemandPlan createCondition, String monthPlanVersion, Map<String, List<MdmProductStock>> finishedProductStockMap);
    /**
     * 将分配时的成品库存记录到库存版本表中(以需求版本号的维度)；
     * @param createCondition 需求参数
     * @param finishedProductStockMap 成品库存记录
     */
    void insertBatchData(MpProductionPrediction createCondition, Map<String, List<MdmProductStock>> finishedProductStockMap);
}
