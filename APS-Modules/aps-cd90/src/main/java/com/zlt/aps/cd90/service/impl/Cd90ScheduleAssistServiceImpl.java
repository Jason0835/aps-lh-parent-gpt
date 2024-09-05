package com.zlt.aps.cd90.service.impl;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleAssist;
import com.zlt.aps.cd90.mapper.Cd90ScheduleAssistMapper;
import com.zlt.aps.cd90.service.Cd90ScheduleAssistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 90度裁断外协排程结果Service业务层处理
 *
 * @author chen
 * @date 2022-02-16
 */
@Service
public class Cd90ScheduleAssistServiceImpl implements Cd90ScheduleAssistService {
    @Autowired
    private Cd90ScheduleAssistMapper cd90ScheduleAssistMapper;

    /**
     * 查询90度裁断外协排程结果列表
     *
     * @param cd90ScheduleAssist 90度裁断外协排程结果
     * @return 90度裁断外协排程结果
     */
    @Override
    public List<Cd90ScheduleAssist> selectCd90ScheduleAssistList(Cd90ScheduleAssist cd90ScheduleAssist) {
        return cd90ScheduleAssistMapper.selectCd90ScheduleAssistList(cd90ScheduleAssist);
    }
}
