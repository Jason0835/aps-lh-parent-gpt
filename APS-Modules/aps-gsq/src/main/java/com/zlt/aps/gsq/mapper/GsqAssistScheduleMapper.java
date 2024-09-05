package com.zlt.aps.gsq.mapper;

import com.zlt.aps.gsq.api.domain.entity.GsqAssistSchedule;

import java.util.List;

/**
 * 钢丝圈外协排程结果Mapper接口
 *
 * @author chen
 * @date 2022-02-15
 */
public interface GsqAssistScheduleMapper {

    /**
     * 查询钢丝圈外协排程结果列表
     *
     * @param gsqAssistSchedule 钢丝圈外协排程结果
     * @return 钢丝圈外协排程结果集合
     */
    public List<GsqAssistSchedule> selectGsqAssistScheduleList(GsqAssistSchedule gsqAssistSchedule);
}
