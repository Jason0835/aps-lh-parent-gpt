package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90DepthConfig;
import org.apache.ibatis.annotations.Mapper;

/** 直裁自动排程读取备库深度配置的只读Mapper。 */
@Mapper
public interface Cd90EngineDepthConfigMapper extends BaseMapper<Cd90DepthConfig> {
}
