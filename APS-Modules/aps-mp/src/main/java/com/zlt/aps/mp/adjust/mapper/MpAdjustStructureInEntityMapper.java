package com.zlt.aps.mp.adjust.mapper;

import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureInMapper.java
 * 描    述：调整-结构内调整记录Mapper接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MpAdjustStructureInEntityMapper extends CommBaseMapper<MpAdjustStructureIn> {

    /**
     * 查询版本列表
     * @param queryVO 查询参数
     * @return 结果
     */
    List<MpAdjustStructureIn> getVersionList(MpAdjustStructureIn queryVO);


}
