package com.zlt.aps.tm.service;

import com.zlt.aps.tm.api.domain.dto.TmAssistScheduleDto;

import java.util.List;

/**
 * 胎面外协排程结果Service接口
 *
 * @author chen
 * @date 2022-02-15
 */
public interface TmAssistScheduleService {

    /**
     * 查询胎面外协排程结果列表
     *
     * @param tmAssistSchedule 胎面外协排程结果
     * @return 胎面外协排程结果集合
     */
    public List<TmAssistScheduleDto> selectTmAssistScheduleList(TmAssistScheduleDto tmAssistSchedule);
}
