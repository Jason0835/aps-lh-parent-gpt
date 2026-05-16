package com.zlt.aps.cx.service;

import com.zlt.aps.cx.vo.ScheduleAdjustResultVo;

/**
 * 成型排程计划调整服务接口
 *
 * <p>负责排程执行后的动态调整，包括：
 * <ul>
 *   <li>交班库存时长调整计划车数</li>
 *   <li>计划滚动调整（胎面供应判断+顺位重置）</li>
 *   <li>8个班滚动调整逻辑</li>
 * </ul>
 *
 * <p>该服务由外部定时任务调用，不在本模块中实现定时触发逻辑。
 *
 * @author APS Team
 */
public interface ScheduleAdjustService {

    /**
     * 交班库存时长调整计划车数
     *
     * <p>当交班剩余胎胚库存可供硫化时长 < 阈值时补1车；
     * 补车后对同班次内库存时长最多的胎胚减1车。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @param shiftClass   当前班次（如 CLASS1~CLASS8）
     * @return 调整结果
     */
    ScheduleAdjustResultVo adjustByStockHours(String factoryCode, String scheduleDate, String shiftClass);

    /**
     * 计划滚动调整
     *
     * <p>在交接班前自动进行计划滚动调整：
     * <ul>
     *   <li>重置当前及后续班次所有计划的顺位</li>
     *   <li>计算每个车次的成型预计开始/结束时间</li>
     *   <li>结合胎面库存判断胎面供应情况</li>
     *   <li>胎面供应不上则顺位后移</li>
     * </ul>
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @param shiftClass   当前班次
     * @return 调整结果
     */
    ScheduleAdjustResultVo rollingAdjust(String factoryCode, String scheduleDate, String shiftClass);

    /**
     * 获取当前应调整的班次范围（8班滚动逻辑）
     *
     * <p>规则：
     * <ul>
     *   <li>T日早班→T日中班+T+1日夜/早/中+T+2日夜/早/中</li>
     *   <li>T日中班→T+1日夜/早/中+T+2日夜/早/中</li>
     *   <li>T+1日夜班→T+1日早/中+T+2日夜/早/中</li>
     *   <li>T+1日早班→执行新的T+1日版本的8个班计划</li>
     * </ul>
     *
     * @param currentShiftClass 当前班次（CLASS1~CLASS8）
     * @return 需要调整的班次列表
     */
    java.util.List<String> getAdjustShiftRange(String currentShiftClass);

    /**
     * 以机台维度滚动重排程
     *
     * <p>三阶段逻辑：
     * <ol>
     *   <li><b>阶段A（班次1~N）</b>：应用已完成班次的 FINISH_QTY，更新库存和成型余量</li>
     *   <li><b>阶段B（班次N+1）</b>：基于交班库存时长检查，补1车/减1车（跳过收尾/停产/试制任务）</li>
     *   <li><b>阶段C（班次N+2~8）</b>：逐班次重新排程（仅first-fit，不均衡）</li>
     * </ol>
     *
     * @param factoryCode        工厂编码
     * @param scheduleDate       排程日期
     * @param triggerShiftClass  触发班次（如 CLASS3，已完成班次=1~3，调整班次=4，重排班次=5~8）
     * @param machineCode        目标成型机台编码
     * @return 调整结果
     */
    ScheduleAdjustResultVo rescheduleByMachine(String factoryCode, String scheduleDate,
                                               String triggerShiftClass, String machineCode);
}
