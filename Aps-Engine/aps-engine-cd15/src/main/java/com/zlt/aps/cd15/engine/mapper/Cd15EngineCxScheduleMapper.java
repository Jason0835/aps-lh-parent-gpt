package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程成型排程只读 Mapper。
 */
@Mapper
public interface Cd15EngineCxScheduleMapper extends BaseMapper<CxScheduleResult> {
}