package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程施工信息只读 Mapper。
 */
@Mapper
public interface Cd15EngineConstructionMapper extends BaseMapper<MdmConstructionInfo> {
}