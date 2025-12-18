package com.zlt.aps.monthplan.demand.mapper;

import java.util.List;
import java.util.Collection;
import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpDemandPlanMapper.java
 * 描    述：需求计划Mapper接口
 *@author yelq
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

public interface DpDemandPlanEntityMapper extends IBaseMapper<DpDemandPlan>
{
    /**
     * 查询需求计划
     * 
     * @param id 需求计划主键
     * @return 需求计划
     */
    DpDemandPlan selectDpDemandPlanById(Long id);

    /**
     * 查询需求计划列表
     * 
     * @param DpDemandPlan 需求计划
     * @return 需求计划集合
     */
    List<DpDemandPlan> selectDpDemandPlanList(DpDemandPlan DpDemandPlan);

    /**
     * 批量查询需求计划列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 需求计划集合
     */
    List<DpDemandPlan> selectDpDemandPlanByIds(List<Long> ids);

    /**
     * 删除需求计划
     * 
     * @param id 需求计划主键
     * @return 结果
     */
    int deleteDpDemandPlanById(Long id);

    /**
     * 批量删除需求计划
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteDpDemandPlanByIds(Long[] ids);
}
