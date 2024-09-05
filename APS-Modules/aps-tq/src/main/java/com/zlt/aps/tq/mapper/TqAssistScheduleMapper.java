package com.zlt.aps.tq.mapper;

import com.zlt.aps.tq.api.domain.entity.TqAssistSchedule;

import java.util.List;

/**
 * 胎圈外协排程结果Mapper接口
 *
 * @author chen
 * @date 2022-02-16
 */
public interface TqAssistScheduleMapper {

    /**
     * 查询胎圈外协排程结果列表
     *
     * @param tqAssistSchedule 胎圈外协排程结果
     * @return 胎圈外协排程结果集合
     */
    public List<TqAssistSchedule> selectTqAssistScheduleList(TqAssistSchedule tqAssistSchedule);
}
