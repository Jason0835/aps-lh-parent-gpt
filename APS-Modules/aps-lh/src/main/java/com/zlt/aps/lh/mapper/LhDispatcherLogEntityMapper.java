package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import com.zlt.aps.lh.api.domain.vo.LhDispatcherLogVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhDispatcherLogMapper.java
 * 描    述：硫化调度员排程操作日志Mapper接口
 *@author zlt
 *@date 2025-03-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface LhDispatcherLogEntityMapper extends CommBaseMapper<LhDispatcherLog> {

    /**
     * 查询是否有变更记录
     *
     * @param queryVO        排程日期
     * @param scheduleIdList 排程ID列表
     * @return 结果
     */
    public List<LhDispatcherLogVo> selectIsChangeList(@Param("queryVo") LhDispatcherLog queryVO,
                                                      @Param("scheduleIdList") List<Long> scheduleIdList);
}
