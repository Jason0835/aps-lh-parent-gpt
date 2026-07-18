package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程施工信息只读Mapper。
 */
@Mapper
public interface Cd15EngineConstructionMapper extends BaseMapper<MdmConstructionInfo> {
}
