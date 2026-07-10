package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程 GDYY 库存只读 Mapper。
 */
@Mapper
public interface Cd15EngineGdyyStockMapper extends BaseMapper<GdyyStock> {
}