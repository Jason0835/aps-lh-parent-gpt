package com.zlt.aps.xwyy.mapper;

import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleAssist;

import java.util.List;

/**
 * 纤维压延外协排程结果Mapper接口
 *
 * @author chen
 * @date 2022-02-16
 */
public interface XwyyScheduleAssistMapper {

    /**
     * 查询纤维压延外协排程结果列表
     *
     * @param xwyyScheduleAssist 纤维压延外协排程结果
     * @return 纤维压延外协排程结果集合
     */
    public List<XwyyScheduleAssist> selectXwyyScheduleAssistList(XwyyScheduleAssist xwyyScheduleAssist);
}
