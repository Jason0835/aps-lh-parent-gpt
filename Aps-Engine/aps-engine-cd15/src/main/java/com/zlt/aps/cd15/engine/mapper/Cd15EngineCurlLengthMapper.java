package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程卷曲长度只读 Mapper。
 */
@Mapper
public interface Cd15EngineCurlLengthMapper extends BaseMapper<Cd15CurlLength> {
}