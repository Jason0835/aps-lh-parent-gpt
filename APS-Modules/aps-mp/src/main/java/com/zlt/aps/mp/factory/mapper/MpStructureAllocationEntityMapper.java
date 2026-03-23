package com.zlt.aps.mp.factory.mapper;

import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpStructureAllocationMapper.java
 * 描    述：排产过程_结构排产Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-29
 */
@Mapper
public interface MpStructureAllocationEntityMapper extends CommBaseMapper<MpStructureAllocation> {

    /**
     * 查询版本列表
     * @param queryVO 查询参数
     * @return 结果
     */
    List<MpStructureAllocation> getVersionList(MpStructureAllocation queryVO);

    /**
     * 获取结构转产表导出数据
     * @param queryVO
     * @return
     */
    List<MpStructureAllocationExportVo> getExportList(MpStructureAllocation queryVO);
}
