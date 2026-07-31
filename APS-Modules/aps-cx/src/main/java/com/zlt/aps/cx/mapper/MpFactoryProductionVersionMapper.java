package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工厂排产版本 Mapper（用于查询次月定稿状态）
 *
 * @author APS Team
 */
@Mapper
public interface MpFactoryProductionVersionMapper extends BaseMapper<MpFactoryProductionVersion> {
}
