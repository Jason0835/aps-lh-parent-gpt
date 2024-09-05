package com.zlt.aps.cd15.service.impl;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleAssist;
import com.zlt.aps.cd15.mapper.Cd15ScheduleAssistMapper;
import com.zlt.aps.cd15.service.Cd15ScheduleAssistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 15度裁断外协排程结果Service业务层处理
 *
 * @author chen
 * @date 2022-02-16
 */
@Service
public class Cd15ScheduleAssistServiceImpl implements Cd15ScheduleAssistService {
    @Autowired
    private Cd15ScheduleAssistMapper cd15ScheduleAssistMapper;

    /**
     * 查询15度裁断外协排程结果列表
     *
     * @param cd15ScheduleAssist 15度裁断外协排程结果
     * @return 15度裁断外协排程结果
     */
    @Override
    public List<Cd15ScheduleAssist> selectCd15ScheduleAssistList(Cd15ScheduleAssist cd15ScheduleAssist) {
        return cd15ScheduleAssistMapper.selectCd15ScheduleAssistList(cd15ScheduleAssist);
    }
}
