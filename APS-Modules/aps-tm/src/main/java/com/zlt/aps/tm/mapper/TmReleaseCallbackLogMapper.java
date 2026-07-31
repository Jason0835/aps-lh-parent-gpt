package com.zlt.aps.tm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.domain.TmReleaseCallbackLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎面排程发布MES反馈去重日志 Mapper。
 */
@Mapper
public interface TmReleaseCallbackLogMapper extends BaseMapper<TmReleaseCallbackLog> {
}
