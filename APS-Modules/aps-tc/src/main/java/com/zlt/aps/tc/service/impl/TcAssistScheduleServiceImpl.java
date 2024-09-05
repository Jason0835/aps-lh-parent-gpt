package com.zlt.aps.tc.service.impl;

import com.zlt.aps.tc.api.domain.entity.TcAssistSchedule;
import com.zlt.aps.tc.mapper.TcAssistScheduleMapper;
import com.zlt.aps.tc.service.TcAssistScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 胎侧外协排程结果Service业务层处理
 *
 * @author chen
 * @date 2022-02-15
 */
@Service
public class TcAssistScheduleServiceImpl implements TcAssistScheduleService {

    @Autowired
    private TcAssistScheduleMapper tcAssistScheduleMapper;


    /**
     * 查询胎侧外协排程结果列表
     *
     * @param tcAssistSchedule 胎侧外协排程结果
     * @return 胎侧外协排程结果
     */
    @Override
    public List<TcAssistSchedule> selectTcAssistScheduleList(TcAssistSchedule tcAssistSchedule) {
        return tcAssistScheduleMapper.selectTcAssistScheduleList(tcAssistSchedule);
    }
}
