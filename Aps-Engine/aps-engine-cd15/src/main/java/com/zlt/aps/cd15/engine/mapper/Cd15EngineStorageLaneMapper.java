package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程6点库排资源只读Mapper。
 */
@Mapper
public interface Cd15EngineStorageLaneMapper extends BaseMapper<Cd15StorageLaneLimit> {
}
