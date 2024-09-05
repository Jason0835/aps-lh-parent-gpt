package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcAssistSchedule;

import java.util.List;

/**
 * 胎侧外协排程结果Mapper接口
 *
 * @author chen
 * @date 2022-02-15
 */
public interface TcAssistScheduleMapper {

    /**
     * 查询胎侧外协排程结果列表
     *
     * @param tcAssistSchedule 胎侧外协排程结果
     * @return 胎侧外协排程结果集合
     */
    public List<TcAssistSchedule> selectTcAssistScheduleList(TcAssistSchedule tcAssistSchedule);
}
