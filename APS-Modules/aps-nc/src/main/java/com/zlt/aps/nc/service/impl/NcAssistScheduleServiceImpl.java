package com.zlt.aps.nc.service.impl;

import com.zlt.aps.nc.api.domain.entity.NcAssistSchedule;
import com.zlt.aps.nc.mapper.NcAssistScheduleMapper;
import com.zlt.aps.nc.service.NcAssistScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内衬胶外协排程结果Service业务层处理
 *
 * @author chen
 * @date 2022-02-15
 */
@Service
public class NcAssistScheduleServiceImpl implements NcAssistScheduleService {
    @Autowired
    private NcAssistScheduleMapper ncAssistScheduleMapper;

    /**
     * 查询内衬胶外协排程结果列表
     *
     * @param ncAssistSchedule 内衬胶外协排程结果
     * @return 内衬胶外协排程结果
     */
    @Override
    public List<NcAssistSchedule> selectNcAssistScheduleList(NcAssistSchedule ncAssistSchedule) {
        return ncAssistScheduleMapper.selectNcAssistScheduleList(ncAssistSchedule);
    }
}
