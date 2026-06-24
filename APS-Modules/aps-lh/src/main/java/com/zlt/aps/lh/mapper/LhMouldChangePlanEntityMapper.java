package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhMoldAlterPlanFinish;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模具交替计划Mapper
 *
 * @author APS Team
 * @since 2026/04/01
 */
@Mapper
public interface LhMouldChangePlanEntityMapper extends CommBaseMapper<LhMouldChangePlan> {

    /**
     * 批量插入模具交替计划
     *
     * @param list 模具交替计划列表
     * @return 插入记录数
     */
    int insertBatch(@Param("list") List<LhMouldChangePlan> list);

    /**
     * 根据模具交替计划完成回报批量更新模具交替完成状态
     * 匹配条件：factoryCode + scheduleDate + orderNo + lhMachineCode + leftRightMould + isDelete=0
     *
     * @param list 已完成的模具交替计划完成回报列表
     * @return 更新记录数
     */
    int batchUpdateMouldStatusByFinish(@Param("list") List<LhMoldAlterPlanFinish> list);

}
