/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DayTypeBlockReverseSelectionDirective;
import com.zlt.aps.lh.engine.strategy.support.SpecifiedMachineScheduleResult;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 换活字块排产子策略接口
 *
 * @author APS
 */
public interface ITypeBlockProductionStrategy {

    /**
     * 执行换活字块排产
     *
     * @param context 排程上下文
     */
    void scheduleTypeBlockChange(LhScheduleContext context);

    /**
     * 按天换活字块机台反选匹配：给定当天候选机台与当天待排物料，返回稳定有序的机台→物料配对。
     *
     * <p>该方法只做无副作用匹配：机台顺序完全复用现有换活字块排序，物料按调用方传入的
     * 当天 S4.5 优先级顺序取首位；每个机台只锁定一个物料、每个物料需求只被一台机台锁定，
     * 保证结果稳定、确定且不可重复占用。实际切换时间、首检、班次计划量、机台收尾时间和
     * 物料账本仍由 S4.5 新增主链统一计算，本方法不写入任何排程资源。</p>
     *
     * @param context 排程上下文
     * @param scheduleDate 反选所属业务日
     * @param dayMaterials 当天待排物料（已按当天 S4.5 优先级排序，不含提前生产物料）
     * @param dayMachines 当天候选机台（已通过现有硬性过滤且可开产时间落在当天）
     * @return 稳定有序的按天换活字块反选指令；无匹配时返回空列表
     */
    default List<DayTypeBlockReverseSelectionDirective> matchDayTypeBlockReversePairs(
            LhScheduleContext context,
            LocalDate scheduleDate,
            List<SkuScheduleDTO> dayMaterials,
            List<MachineScheduleDTO> dayMachines) {
        return java.util.Collections.emptyList();
    }

    /**
     * 在指定机台尝试换活字块排产。
     *
     * <p>该入口只为历史交替计划反选提供指定机台作用域，实际候选判断、切换资源、
     * 首检、班次产能、结果和账本更新仍复用正式换活字块主链。</p>
     *
     * @param context 排程上下文
     * @param machine 指定机台
     * @param sku 目标SKU
     * @param mappedShiftIndex 历史班次映射后的当前班次
     * @return 指定机台换活字块执行结果
     */
    default SpecifiedMachineScheduleResult tryScheduleSpecifiedMachine(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            int mappedShiftIndex) {
        return SpecifiedMachineScheduleResult.notApplicable("当前换活字块策略不支持指定机台模式");
    }

    /**
     * 无副作用判断特殊材料在指定续作机台是否适用换活字块。
     *
     * <p>置换预演需要先确定切换时长，只有命中同胎胚、同模具等现有换活字块条件时，
     * 才能使用换活字块时长；实现不得在本方法内写入排程结果或资源账本。</p>
     *
     * @param context 排程上下文
     * @param machine 特殊材料准备接管的续作机台
     * @param sku 特殊材料 SKU
     * @return true-适用换活字块时长；false-按正规换模时长预演
     */
    default boolean isSpecialMaterialSubstitutionTypeBlockApplicable(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku) {
        return false;
    }

    /**
     * 在特殊材料置换选定的续作机台上尝试换活字块排产。
     *
     * <p>历史反选必须锁定历史班次；特殊材料置换只限制“不得早于最终预演时间”，
     * 实际切换仍可按换模均衡、停机和禁换模规则继续顺延。</p>
     *
     * @param context 排程上下文
     * @param machine 特殊材料准备接管的续作机台
     * @param sku 特殊材料 SKU
     * @param earliestSwitchTime 置换预演得出的最早允许切换时间
     * @return 指定机台换活字块执行结果；不满足换活字块关系时返回不适用
     */
    default SpecifiedMachineScheduleResult tryScheduleSpecialMaterialSubstitution(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            Date earliestSwitchTime) {
        return SpecifiedMachineScheduleResult.notApplicable("当前换活字块策略不支持特殊材料置换模式");
    }
}
