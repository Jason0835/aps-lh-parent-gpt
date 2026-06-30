package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直裁自动排程读取XWYY计划库存的只读Mapper。
 */
@Mapper
public interface Cd90EngineXwyyScheduleResultMapper extends BaseMapper<XwyyScheduleResult> {
}
