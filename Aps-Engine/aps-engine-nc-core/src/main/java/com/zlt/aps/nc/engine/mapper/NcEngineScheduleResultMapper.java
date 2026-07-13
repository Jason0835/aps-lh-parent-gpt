package com.zlt.aps.nc.engine.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;

/**
 * 内衬排程结果 Mapper
 */
public interface NcEngineScheduleResultMapper extends CommBaseMapper<NcScheduleResult> {

    /**
     * 物理删除指定日期的排产记录
     * @param scheduleDate 排程日期
     * @return 删除行数
     */
    int deleteNcSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询各内衬代码最近一次有排产量的排产日期（联合主表和日志表）
     * @param factoryCode 工厂编码
     * @param paddingCodes 内衬代码列表
     * @return 内衬代码 -> 最近排产日期 的映射列表
     */
    List<Map<String, Object>> selectLastScheduleDate(@Param("factoryCode") String factoryCode,
            @Param("paddingCodes") List<String> paddingCodes);
}
