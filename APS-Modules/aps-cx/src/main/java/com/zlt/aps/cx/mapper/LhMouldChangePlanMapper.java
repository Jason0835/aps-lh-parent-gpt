package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模具交替计划 Mapper（成型模块读取硫化侧换模/换活字块/喷砂清洗计划）。
 *
 * <p>用于成型余量、成型日计划导出的备注列，实时从硫化侧模具交替计划计算备注。</p>
 *
 * @author APS Team
 */
@Mapper
public interface LhMouldChangePlanMapper extends BaseMapper<LhMouldChangePlan> {
}
