package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleAssist;

import java.util.List;

/**
 * 90度裁断外协排程结果Service接口
 *
 * @author chen
 * @date 2022-02-16
 */
public interface Cd90ScheduleAssistService {

    /**
     * 查询90度裁断外协排程结果列表
     *
     * @param cd90ScheduleAssist 90度裁断外协排程结果
     * @return 90度裁断外协排程结果集合
     */
    public List<Cd90ScheduleAssist> selectCd90ScheduleAssistList(Cd90ScheduleAssist cd90ScheduleAssist);
}
