package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;

import java.util.List;

/**
 * 成型排程核心算法契约。
 *
 * <p>本接口仅声明 {@link #executeSchedule} 入口；编排逻辑在
 * {@link com.zlt.aps.cx.service.impl.CoreScheduleAlgorithmServiceImpl}。
 * 业务调用链：{@link com.zlt.aps.cx.service.impl.ScheduleServiceImpl#executeSchedule}
 * 构建 {@link ScheduleContextVo} 后委托本接口执行。
 *
 * @author APS Team
 * @see com.zlt.aps.cx.service.impl.CoreScheduleAlgorithmServiceImpl
 */
public interface CoreScheduleAlgorithmService {

    /**
     * 执行完整成型排程（多天多班次）。
     *
     * <p><b>外层循环</b>：按排程天/班次迭代（默认约 3 天、8 个班次），每天调用
     * {@code executeShiftSchedule} 完成「分组 -> 三类 Processor -> 精排」。
     *
     * <p><b>天间滚动</b>：每班次结束后更新 context（库存消耗、成型/硫化余量、在机胎胚映射等），
     * 供下一班次 TaskGroupService 使用。
     *
     * <p><b>输出聚合</b>：将各班次 {@link com.zlt.aps.cx.vo.ShiftProductionResult}
     * 按「机台 + 胎胚 + 物料」维度合并为 {@link CxScheduleResult}，
     * 每条记录的 CLASS1~CLASS8 对应该物料在 8 个班次上的计划条数。
     *
     * @param context 已由 ScheduleServiceImpl 加载完毕的排程上下文（机台/物料/参数/硫化任务/库存等）
     * @return 持久化前的排程结果列表
     */
    List<CxScheduleResult> executeSchedule(ScheduleContextVo context);
}
