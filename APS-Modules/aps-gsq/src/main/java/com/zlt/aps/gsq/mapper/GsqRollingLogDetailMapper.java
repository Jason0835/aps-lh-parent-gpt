package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqRollingLogDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 钢丝圈排程滚动更新日志明细Mapper接口
 *
 * @author APS
 */
@Mapper
public interface GsqRollingLogDetailMapper extends BaseMapper<GsqRollingLogDetail> {
}
