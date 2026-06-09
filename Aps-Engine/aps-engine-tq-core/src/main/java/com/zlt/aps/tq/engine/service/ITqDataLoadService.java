package com.zlt.aps.tq.engine.service;

import com.zlt.aps.tq.engine.context.TqScheduleContext;

/**
 * 胎圈排程数据加载服务接口。
 *
 * <p>定义在 aps-engine-tq-core 层，由 aps-engine-tq 层实现。
 * 解决core层Handler无法直接依赖engine-tq层Service的依赖方向问题。</p>
 *
 * <p>职责：将所有需要从外部Service查询的基础数据统一加载到Context中，
 * 包括：工序参数、机台列表、定点机台、口型板、库存、损耗率、月度剩余等。</p>
 *
 * @author APS
 */
public interface ITqDataLoadService {

    /**
     * 加载全部基础数据到排程上下文。
     *
     * <p>该方法在S1阶段被调用，负责将所有排程所需的基础数据
     * 从各个Service查询后写入Context的对应字段。</p>
     *
     * <p>加载的数据包括：</p>
     * <ul>
     *   <li>工序参数（13项）</li>
     *   <li>排程基础数据（从成型排程统计）</li>
     *   <li>施工信息</li>
     *   <li>外协规格</li>
     *   <li>机台列表、定点机台、口型板机台</li>
     *   <li>库存、预计库存、昨日中班计划</li>
     *   <li>损耗率</li>
     *   <li>月度剩余</li>
     *   <li>批次号</li>
     * </ul>
     *
     * @param context 排程上下文，必须已设置scheduleDate
     */
    void loadAllData(TqScheduleContext context);
}
