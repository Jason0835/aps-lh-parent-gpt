package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import org.apache.ibatis.annotations.Mapper;

/** 硫化排程读取结构与胎胚的首班整车条数配置，复用成型实体及框架逻辑删除。 */
@Mapper
public interface CxStructureTreadConfigMapper extends BaseMapper<CxStructureTreadConfig> {
}
