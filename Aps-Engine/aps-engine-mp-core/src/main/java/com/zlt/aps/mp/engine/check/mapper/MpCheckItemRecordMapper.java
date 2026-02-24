package com.zlt.aps.mp.engine.check.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpCheckItemRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpCheckItemRecordMapper.java
 * 描    述：S2-1202 检测项记录Mapper接口
 *@author hsc
 *@date 2026-01-29
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hsc
 *     修改内容：...
 */
@Mapper
public interface MpCheckItemRecordMapper extends BaseMapper<MpCheckItemRecord> {

    /**
     * 查询S2-1202 检测项记录列表
     *
     * @param mpCheckItemRecord S2-1202 检测项记录
     * @return S2-1202 检测项记录集合
     */
    public List<MpCheckItemRecord> selectMpCheckItemRecordList(MpCheckItemRecord mpCheckItemRecord);
}
