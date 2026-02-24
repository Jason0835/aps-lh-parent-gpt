package com.zlt.aps.mdm.mapper;

import com.zlt.aps.mdm.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmAreaCapaAllocationMapper.java
 * 描    述：区域产能分配Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@Mapper
public interface MdmAreaCapaAllocationEntityMapper extends CommBaseMapper<MdmAreaCapaAllocation> {

    /**
     * 复制，将源分厂年月复制到目标分厂年月
     *
     * @param entity 源参数
     * @return 源参数
     */
    int copy(MdmAreaCapaAllocation entity);
}
