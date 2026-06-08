package com.zlt.aps.mp.factory.mapper;

import com.zlt.aps.mp.api.domain.entity.MpFinalStructureAllocationLog;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpFinalStructureAllocationLogEntityMapper.java
 * 描    述：S2-0409.定稿版本结构排产表备份日志Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-05-19
 */
@Mapper
public interface MpFinalStructureAllocationLogEntityMapper extends CommBaseMapper<MpFinalStructureAllocationLog> {

    /**
     * 获取结构转产表导出数据
     * @param queryVO
     * @return
     */
    List<MpStructureAllocationExportVo> getExportList(MpStructureAllocation queryVO);
}
