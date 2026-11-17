package com.zlt.aps.common.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提前生产公共查询使用的定稿月计划访问接口。
 *
 * @author APS
 * @date 2026/11/16
 */
@Mapper
public interface EarlyProductionMonthPlanMapper extends BaseMapper<FactoryMonthPlanProductionFinalResult> {
}
