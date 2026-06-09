package com.zlt.aps.dj.service;

import com.zlt.aps.dj.api.domain.entity.DjAssistSchedule;

import java.util.List;

/**
 * 垫胶胶外协排程结果Service接口
 *
 * @author chen
 * @date 2022-02-15
 */
public interface DjAssistScheduleService {

    /**
     * 查询垫胶胶外协排程结果列表
     *
     * @param ncAssistSchedule 垫胶胶外协排程结果
     * @return 垫胶胶外协排程结果集合
     */
    public List<DjAssistSchedule> selectNcAssistScheduleList(DjAssistSchedule ncAssistSchedule);
}
