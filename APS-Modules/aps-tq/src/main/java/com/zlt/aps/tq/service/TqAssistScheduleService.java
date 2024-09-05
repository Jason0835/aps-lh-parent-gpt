package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqAssistSchedule;

import java.util.List;

/**
 * 胎圈外协排程结果Service接口
 *
 * @author chen
 * @date 2022-02-16
 */
public interface TqAssistScheduleService {

    /**
     * 查询胎圈外协排程结果列表
     *
     * @param tqAssistSchedule 胎圈外协排程结果
     * @return 胎圈外协排程结果集合
     */
    public List<TqAssistSchedule> selectTqAssistScheduleList(TqAssistSchedule tqAssistSchedule);

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    public byte[] export(List<TqAssistSchedule> list);
}
