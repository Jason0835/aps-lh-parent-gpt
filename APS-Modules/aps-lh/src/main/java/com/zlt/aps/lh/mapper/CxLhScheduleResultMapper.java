package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 硫化排程结果Mapper（使用aps-cx-lh-api实体，包含8班数据）
 *
 * @author APS Team
 * @since 2.0.0
 */
@Mapper
public interface CxLhScheduleResultMapper extends BaseMapper<LhScheduleResult> {
}
