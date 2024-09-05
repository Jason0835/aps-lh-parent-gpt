package com.zlt.aps.tm.service.impl;

import com.zlt.aps.tm.api.domain.dto.TmAssistScheduleDto;
import com.zlt.aps.tm.mapper.TmAssistScheduleMapper;
import com.zlt.aps.tm.service.TmAssistScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 胎面外协排程结果Service业务层处理
 *
 * @author chen
 * @date 2022-02-15
 */
@Service
public class TmAssistScheduleServiceImpl implements TmAssistScheduleService {
    @Autowired
    private TmAssistScheduleMapper tmAssistScheduleMapper;

    /**
     * 查询胎面外协排程结果列表
     *
     * @param tmAssistSchedule 胎面外协排程结果
     * @return 胎面外协排程结果
     */
    @Override
    public List<TmAssistScheduleDto> selectTmAssistScheduleList(TmAssistScheduleDto tmAssistSchedule) {
        return tmAssistScheduleMapper.selectTmAssistScheduleList(tmAssistSchedule);
    }
}
