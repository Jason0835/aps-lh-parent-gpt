package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.AutoScheduleLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 自动排程日志Mapper接口
 * 
 * @author zlt
 * @date 2021-07-16
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
