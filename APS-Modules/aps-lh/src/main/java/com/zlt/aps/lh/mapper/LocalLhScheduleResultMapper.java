package com.zlt.aps.lh.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 硫化排程结果本地表Mapper（仅用于导出查询走本地表）。
 *
 * <p>通过 {@code @DS("localhost")} 指定数据源 localhost，用于排程结果导出时查询本地表，
 * 不影响主数据源上的排程业务读写。请勿在此 Mapper 注册其它排程业务方法。</p>
 *
 * @author APS
 */
@Mapper
@DS("localhost")
public interface LocalLhScheduleResultMapper extends BaseMapper<LhScheduleResult> {
}
