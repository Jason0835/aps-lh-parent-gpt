package com.zlt.aps.dj.engine.mapper;

import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.Set;

/**
 * 垫胶排程结果 Mapper
 */
public interface DjEngineScheduleResultMapper extends CommBaseMapper<DjScheduleResult> {

    /**
     * 物理删除指定日期的排产记录
     * @param scheduleDate 排程日期
     * @return 删除行数
     */
    int deleteDjSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询指定时间窗口内有排产量的垫胶代码集合
     * @param factoryCode 工厂编码
     * @param thresholdDate 时间窗口下限（排产日 - 新规格天数阈值）
     * @param scheduleDate 时间窗口上限（排产日）
     * @param paddingCodes 垫胶代码集合
     * @return 窗口内有排产记录的垫胶代码集合
     */
    Set<String> selectLastScheduleDate(@Param("factoryCode") String factoryCode,
            @Param("thresholdDate") Date thresholdDate,
            @Param("scheduleDate") Date scheduleDate,
            @Param("paddingCodes") Set<String> paddingCodes);
}
