package com.zlt.mix.common.engine.mapper;

import com.zlt.mix.common.engine.domain.AutoScheduleLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程日志Mapper接口
 * 
 */
@Mapper
public interface AutoScheduleLogMapper 
{

    /**
     * 新增自动排程日志
     * 
     * @param autoScheduleLog 自动排程日志
     * @return 结果
     */
    int insertAutoScheduleLog(AutoScheduleLog autoScheduleLog);
}
