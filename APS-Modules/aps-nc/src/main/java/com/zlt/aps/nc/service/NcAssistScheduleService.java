package com.zlt.aps.nc.service;

import com.zlt.aps.nc.api.domain.entity.NcAssistSchedule;

import java.util.List;

/**
 * 内衬胶外协排程结果Service接口
 *
 * @author chen
 * @date 2022-02-15
 */
public interface NcAssistScheduleService {

    /**
     * 查询内衬胶外协排程结果列表
     *
     * @param ncAssistSchedule 内衬胶外协排程结果
     * @return 内衬胶外协排程结果集合
     */
    public List<NcAssistSchedule> selectNcAssistScheduleList(NcAssistSchedule ncAssistSchedule);
}
