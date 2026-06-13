package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程6点库排资源只读Mapper。
 */
@Mapper
public interface Cd90EngineStorageLaneMapper extends BaseMapper<Cd90StorageLaneLimit> {
}
