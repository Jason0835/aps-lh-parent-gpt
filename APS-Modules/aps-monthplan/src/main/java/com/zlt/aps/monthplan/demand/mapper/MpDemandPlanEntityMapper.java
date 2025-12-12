package com.zlt.aps.monthplan.demand.mapper;

import java.util.List;
import java.util.Collection;
import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpDemandPlan;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpDemandPlanMapper.java
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

public interface MpDemandPlanEntityMapper extends IBaseMapper<MpDemandPlan>
{
    /**
     * 查询需求计划
     * 
     * @param id 需求计划主键
     * @return 需求计划
     */
    public MpDemandPlan selectMpDemandPlanById(Long id);

    /**
     * 查询需求计划列表
     * 
     * @param mpDemandPlan 需求计划
     * @return 需求计划集合
     */
    public List<MpDemandPlan> selectMpDemandPlanList(MpDemandPlan mpDemandPlan);

    /**
     * 批量查询需求计划列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 需求计划集合
     */
    public List<MpDemandPlan> selectMpDemandPlanByIds(List<Long> ids);

    /**
     * 删除需求计划
     * 
     * @param id 需求计划主键
     * @return 结果
     */
    public int deleteMpDemandPlanById(Long id);

    /**
     * 批量删除需求计划
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMpDemandPlanByIds(Long[] ids);


    /**
 * 合并操作，如果记录存在则更新，否则新增
 */
    @Override
    public int mergeSql(Collection<? extends MpDemandPlan> collection);
}
