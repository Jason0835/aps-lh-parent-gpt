package com.zlt.aps.dj.mapper;

import java.util.List;

import com.zlt.aps.dj.api.domain.entity.DjAssistSchedule;

/**
 * 垫胶胶外协排程结果Mapper接口
 *
 * @author chen
 * @date 2022-02-15
 */
public interface DjAssistScheduleMapper {

    /**
     * 查询垫胶胶外协排程结果列表
     *
     * @param ncAssistSchedule 垫胶胶外协排程结果
     * @return 垫胶胶外协排程结果集合
     */
    public List<DjAssistSchedule> selectNcAssistScheduleList(DjAssistSchedule ncAssistSchedule);
}
