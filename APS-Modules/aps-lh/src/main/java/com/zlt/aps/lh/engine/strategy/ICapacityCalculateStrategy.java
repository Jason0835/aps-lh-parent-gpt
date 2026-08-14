/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy;

import com.zlt.aps.lh.context.LhScheduleContext;

import java.util.Date;

/**
 * 产能计算策略接口
 * <p>计算硫化班产、开产时间和班次计划量</p>
 *
 * @author APS
 */
public interface ICapacityCalculateStrategy {

    /**
     * 计算硫化班产
     * <p>公式: (班次时间-非生产作业时间) / 硫化时间 向下取整 * 模数</p>
     *
     * @param context       排程上下文
     * @param lhTimeSeconds 硫化时间(秒)
     * @param mouldQty      模数
     * @return 班产量
     */
    int calculateShiftCapacity(LhScheduleContext context, int lhTimeSeconds, int mouldQty);

    /**
     * 计算机台准备就绪时间。
     * <p>本方法只收敛前 SKU 收尾、精度/保养及维修恢复等机台级下限，
     * 不再预先叠加换模、换活字块或首检时长。新增选机会以该时刻为起点，
     * 由统一真实可开产计划继续计算切换、首检、清洗、停机和正式生产时间轴。</p>
     *
     * @param context     排程上下文
     * @param machineCode 机台编号
     * @param endingTime  前SKU收尾时间
     * @return 机台可继续安排切换准备的就绪时间
     */
    Date calculateStartTime(LhScheduleContext context, String machineCode, Date endingTime);

    /**
     * 计算首班计划量
     * <p>公式: (首班次结束时间 - 首班次上机时间) / 硫化时间 向下取整 * 模数</p>
     *
     * @param startTime     开产时间
     * @param shiftEndTime  首班次结束时间
     * @param lhTimeSeconds 硫化时间(秒)
     * @param mouldQty      模数
     * @return 首班计划量
     */
    int calculateFirstShiftQty(Date startTime, Date shiftEndTime, int lhTimeSeconds, int mouldQty);

    /**
     * 计算日硫化量
     * <p>日单模产能 = 24*3600 / 硫化时间; 日硫化量 = 日单模产能 * 模数</p>
     *
     * @param lhTimeSeconds 硫化时间(秒)
     * @param mouldQty      模数
     * @return 日硫化量
     */
    int calculateDailyCapacity(int lhTimeSeconds, int mouldQty);
}
