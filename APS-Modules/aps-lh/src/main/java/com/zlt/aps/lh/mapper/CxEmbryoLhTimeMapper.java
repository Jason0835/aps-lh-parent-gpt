package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 胎胚最早可供硫化时间Mapper
 *
 * @author APS Team
 */
@Mapper
public interface CxEmbryoLhTimeMapper extends BaseMapper<CxEmbryoLhTime> {

}
