package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程成型排程只读Mapper。
 */
@Mapper
public interface Cd90EngineCxScheduleMapper extends BaseMapper<CxScheduleResult> {
}
