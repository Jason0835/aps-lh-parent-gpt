package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排程参数配置Mapper（用于排产小结报表查询成型参数配置）
 *
 * @author APS Team
 */
@Mapper
public interface CxParamConfigMapper extends BaseMapper<CxParamConfig> {
}
