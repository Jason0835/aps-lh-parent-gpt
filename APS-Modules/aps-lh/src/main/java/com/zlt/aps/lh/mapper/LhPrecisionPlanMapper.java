package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 硫化精度计划Mapper接口
 *
 * @author APS Team
 */
public interface LhPrecisionPlanMapper extends BaseMapper<LhPrecisionPlan> {

    /**
     * 查询硫化精度计划列表
     *
     * @param vo 查询条件
     * @return 计划列表
     */
    List<LhPrecisionPlan> selectLhPrecisionPlanList(LhPrecisionPlanVo vo);

    /**
     * 根据机台编码和年份查询计划
     *
     * @param machineCode 机台编码
     * @param year 年份
     * @return 计划
     */
    LhPrecisionPlan selectByMachineCodeAndYear(@Param("machineCode") String machineCode, @Param("year") Integer year);

    /**
     * 查询机台最近一次已完成的计划
     *
     * @param machineCode 机台编码
     * @param year 年份
     * @return 计划
     */
    LhPrecisionPlan selectLastCompletedPlan(@Param("machineCode") String machineCode, @Param("year") Integer year);

    /**
     * 查询待预警的计划列表
     *
     * @param daysToDue 到期天数阈值
     * @return 计划列表
     */
    List<LhPrecisionPlan> selectPendingWarningPlans(@Param("daysToDue") Integer daysToDue);

    /**
     * 批量更新到期天数
     *
     * @return 更新数量
     */
    int batchUpdateDaysToDue();
}
