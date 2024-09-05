package com.zlt.aps.cd15.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleAssist;

import java.util.List;

/**
 * 15度裁断外协排程结果Mapper接口
 *
 * @author chen
 * @date 2022-02-16
 */
public interface Cd15ScheduleAssistMapper {

    /**
     * 查询15度裁断外协排程结果列表
     *
     * @param cd15ScheduleAssist 15度裁断外协排程结果
     * @return 15度裁断外协排程结果集合
     */
    public List<Cd15ScheduleAssist> selectCd15ScheduleAssistList(Cd15ScheduleAssist cd15ScheduleAssist);
}
