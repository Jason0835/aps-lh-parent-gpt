package com.zlt.aps.cx.mapper;

import com.zlt.aps.lh.api.domain.entity.LhDayPlanAdjustRequire;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 硫化日计划调整需求 Mapper（跨模块查询 LH 表）。
 *
 * <p>TODO(后续优化): 当前为跨模块共享数据源直查，
 * 后续统一改为通过 Feign 调用 ILhDayPlanAdjustRequireRemoteService 暴露的
 * {@code getMonthPlanLhDayAdjustList(yearMonth, factoryList, materialCodeList)}
 * 远端服务，以保证定稿版本查找、表过滤等业务逻辑与硫化侧口径一致。
 * </p>
 *
 * @author APS Team
 */
@Mapper
public interface LhDayPlanAdjustMapper extends CommBaseMapper<LhDayPlanAdjustRequire> {
}
