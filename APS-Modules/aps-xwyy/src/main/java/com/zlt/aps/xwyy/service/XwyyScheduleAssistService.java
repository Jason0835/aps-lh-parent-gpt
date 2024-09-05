package com.zlt.aps.xwyy.service;

import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleAssist;

import java.util.List;

/**
 * 纤维压延外协排程结果Service接口
 *
 * @author chen
 * @date 2022-02-16
 */
public interface XwyyScheduleAssistService {

    /**
     * 查询纤维压延外协排程结果列表
     *
     * @param xwyyScheduleAssist 纤维压延外协排程结果
     * @return 纤维压延外协排程结果集合
     */
    public List<XwyyScheduleAssist> selectXwyyScheduleAssistList(XwyyScheduleAssist xwyyScheduleAssist);

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    public byte[] export(List<XwyyScheduleAssist> list);
}
