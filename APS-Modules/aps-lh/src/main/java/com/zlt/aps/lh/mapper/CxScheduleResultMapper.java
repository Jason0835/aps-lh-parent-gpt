package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成型排程结果Mapper（使用aps-cx-lh-api实体，用于排产小结报表查询）
 *
 * @author APS Team
 */
@Mapper
public interface CxScheduleResultMapper extends BaseMapper<CxScheduleResult> {
}
