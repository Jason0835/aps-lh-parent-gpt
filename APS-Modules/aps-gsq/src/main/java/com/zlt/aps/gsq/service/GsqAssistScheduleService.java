package com.zlt.aps.gsq.service;

import com.zlt.aps.gsq.api.domain.entity.GsqAssistSchedule;

import java.util.List;

/**
 * 钢丝圈外协排程结果Service接口
 *
 * @author chen
 * @date 2022-02-15
 */
public interface GsqAssistScheduleService {

    /**
     * 查询钢丝圈外协排程结果列表
     *
     * @param gsqAssistSchedule 钢丝圈外协排程结果
     * @return 钢丝圈外协排程结果集合
     */
    public List<GsqAssistSchedule> selectGsqAssistScheduleList(GsqAssistSchedule gsqAssistSchedule);

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    public byte[] export(List<GsqAssistSchedule> list);
}
