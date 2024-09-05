package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.entity.TcAssistSchedule;

import java.util.List;

/**
 * 胎侧外协排程结果Service接口
 *
 * @author chen
 * @date 2022-02-15
 */
public interface TcAssistScheduleService {

    /**
     * 查询胎侧外协排程结果列表
     *
     * @param tcAssistSchedule 胎侧外协排程结果
     * @return 胎侧外协排程结果集合
     */
    public List<TcAssistSchedule> selectTcAssistScheduleList(TcAssistSchedule tcAssistSchedule);
}
