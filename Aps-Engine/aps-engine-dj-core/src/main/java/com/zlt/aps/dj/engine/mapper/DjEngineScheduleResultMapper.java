package com.zlt.aps.dj.engine.mapper;

import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

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
     * 查询各垫胶代码最近一次有排产量的排产日期（联合主表和日志表）
     * @param factoryCode 工厂编码
     * @param paddingCodes 垫胶代码列表
     * @return 垫胶代码 -> 最近排产日期 的映射列表
     */
    List<Map<String, Object>> selectLastScheduleDate(@Param("factoryCode") String factoryCode,
            @Param("paddingCodes") List<String> paddingCodes);
}
