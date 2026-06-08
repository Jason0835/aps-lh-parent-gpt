package com.zlt.aps.dj.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zlt.aps.dj.api.domain.entity.DjAssistSchedule;
import com.zlt.aps.dj.mapper.DjAssistScheduleMapper;
import com.zlt.aps.dj.service.DjAssistScheduleService;

/**
 * 垫胶胶外协排程结果Service业务层处理
 *
 * @author chen
 * @date 2022-02-15
 */
@Service
public class DjAssistScheduleServiceImpl implements DjAssistScheduleService {
    @Autowired
    private DjAssistScheduleMapper ncAssistScheduleMapper;

    /**
     * 查询垫胶胶外协排程结果列表
     *
     * @param ncAssistSchedule 垫胶胶外协排程结果
     * @return 垫胶胶外协排程结果
     */
    @Override
    public List<DjAssistSchedule> selectNcAssistScheduleList(DjAssistSchedule ncAssistSchedule) {
        return ncAssistScheduleMapper.selectNcAssistScheduleList(ncAssistSchedule);
    }
}
