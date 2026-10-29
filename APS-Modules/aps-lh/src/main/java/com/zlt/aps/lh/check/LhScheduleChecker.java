package com.zlt.aps.lh.check;

import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.check.checker.CapsuleChuckChecker;
import com.zlt.aps.lh.check.checker.EmbryoEndingShiftGapChecker;
import com.zlt.aps.lh.check.checker.IdleShiftChecker;
import com.zlt.aps.lh.check.checker.MouldShellCheckChecker;
import com.zlt.aps.lh.check.checker.SharedEmbryoEndingStockChecker;

import java.util.List;

/**
 * 硫化排程结果合理性校验器接口。
 *
 * <p>每个检查项实现本接口并以 Spring Bean 注册，由
 * {@code LhScheduleCheckServiceImpl} 统一收集执行；单个检查项内部异常
 * 由执行方捕获降级，不阻断其他检查项。</p>
 *
 * <p>编号沿用需求评审时的检查项编号（1/4/5/6/7）。</p>
 *
 * @author APS
 * @date 2026/10/27
 */
public interface LhScheduleChecker {

    /** 检查项编码：空班 */
    String CODE_IDLE_SHIFT = "IDLE_SHIFT";

    /** 检查项编码：收尾班差 */
    String CODE_ENDING_SHIFT_GAP = "ENDING_SHIFT_GAP";

    /** 检查项编码：收尾库存 */
    String CODE_ENDING_STOCK = "ENDING_STOCK";

    /** 检查项编码：模壳 */
    String CODE_MOULD_SHELL = "MOULD_SHELL";

    /** 检查项编码：卡盘 */
    String CODE_CAPSULE_CHUCK = "CAPSULE_CHUCK";

    /** 每日物理班数量上限（class1~class8，班次9的独立计划不纳入校验） */
    int SHIFT_SLOT_COUNT = 8;

    /**
     * 获取检查项编码（唯一标识，对应 {@link LhScheduleCheckResult#getCheckCode()}）。
     *
     * @return 检查项编码
     */
    String getCode();

    /**
     * 获取检查项名称的多语言 key。
     *
     * @return 多语言 key（如 ui.data.column.lhScheduleCheck.idleShift）
     */
    String getNameKey();

    /**
     * 是否强控级别。
     * <p>强控（true）：异常需用户确认后才能继续发布/导出等操作；
     * 提示（false）：仅提示，不阻断。</p>
     *
     * @return true-强控；false-提示
     */
    boolean isHardControl();

    /**
     * 执行校验。
     *
     * <p>实现要求：只读 {@link LhScheduleCheckContext}，不修改上下文数据；
     * 返回的每条异常记录由执行方补齐 checkName/checkLevel/triggerType/checkTime/acknowledged 等公共字段。</p>
     *
     * @param context 校验上下文（含结果集与台账数据）
     * @return 异常记录列表；无异常时返回空列表
     */
    List<LhScheduleCheckResult> check(LhScheduleCheckContext context);
}
