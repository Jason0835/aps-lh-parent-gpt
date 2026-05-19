package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成型精度计划Mapper（用于排产小结报表查询成型精度计划）
 *
 * @author APS Team
 */
@Mapper
public interface CxPrecisionPlanMapper extends BaseMapper<CxPrecisionPlan> {
}
