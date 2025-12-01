package com.zlt.mix.schedule.engine.util.event;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;
import com.zlt.mix.schedule.engine.util.ScheduleEventQueue;
import com.zlt.mix.schedule.engine.util.ShiftClassUtil;
import com.zlt.mix.schedule.engine.vo.GlueScheduleMachineProductVo;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 接续生产事件，用于接续生产的机台机台提前占用
 *
 * @author Liam
 * @since 2025/4/20
 */
public class ContinueProductEvent implements ScheduleEvent {
    // 机台产能
    private GlueScheduleMachineProductVo machineProductVo;
    // 排产记录
    private GlueScheduleResultVo scheduleResult;
    // 预排数量
    private BigDecimal predictProductQty;

    public ContinueProductEvent(GlueScheduleMachineProductVo machineProductVo, BigDecimal predictProductQty, GlueScheduleResultVo scheduleResult) {
        this.machineProductVo = machineProductVo;
        this.predictProductQty = predictProductQty;
        this.scheduleResult = scheduleResult;
    }

    /**
     * 当时时间的
     *
     * @param queue 事件队列
     */
    @Override
    public void excute(ScheduleEventQueue queue) {
        Date currentTime = queue.getCurrentTime();
        Integer currentShiftClass = ShiftClassUtil.getShiftClass(currentTime); // 当前班次为开始时间的所在班次
        // 扣减原料的库存
        GlueScheduleStockPool glueStock = queue.getGlueStock();
        glueStock.subtractChildGlueStock(predictProductQty, scheduleResult.getPmtRecipe());
        // 当前班次的机台如果是可用的，将状态标注为使用使用状态
        if (!machineProductVo.getStatus(currentShiftClass)) {
            return;
        }
        machineProductVo.setState(GlueEngineConstants.MACHINE_STATE_ON);
        machineProductVo.setStartProductTime(currentTime);
        queue.addLog(DateUtils.parseDateToStr("HH:mm:ss", queue.getCurrentTime()) + "|接续排产提前占用机台" + scheduleResult.getGlue()
                + "+" + scheduleResult.getMachineCode() + "+" + scheduleResult.getRecipeTypeName() + "===" + predictProductQty);
    }
}
