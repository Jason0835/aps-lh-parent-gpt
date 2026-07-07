package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程库存快照只读 Mapper。
 */
@Mapper
public interface Cd15EngineStockMapper extends BaseMapper<Cd15Stock> {
}