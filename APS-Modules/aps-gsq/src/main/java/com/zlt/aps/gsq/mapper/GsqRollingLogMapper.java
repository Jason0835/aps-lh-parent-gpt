package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqRollingLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 钢丝圈排程滚动更新日志主表Mapper接口
 *
 * @author APS
 */
@Mapper
public interface GsqRollingLogMapper extends BaseMapper<GsqRollingLog> {
}
