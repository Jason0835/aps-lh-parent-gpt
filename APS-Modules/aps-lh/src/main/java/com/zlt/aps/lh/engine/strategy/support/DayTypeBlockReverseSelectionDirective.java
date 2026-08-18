package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 按天换活字块机台反选指令。
 *
 * <p>由 S4.5 新增排产每天开始时的“换活字块检测 + 机台反选物料”生成，只承载
 * 当天业务日内的机台→物料配对关系与对账信息。本指令不复制、不重算任何换活字块时间，
 * 实际切换时间、首检、班次计划量、机台收尾时间和物料账本仍由 S4.5 新增主链统一计算。
 * 指令生命周期只覆盖当天正常资源竞争阶段：命中后优先落地，阶段结束统一结算并释放机台预留，
 * 避免跨业务日残留导致重复锁定或重复排产。</p>
 *
 * @author APS
 */
@Data
public class DayTypeBlockReverseSelectionDirective implements Serializable {

    /** 反选所属业务日（T/T+1/T+2），用于跨天重新检测和日志对账 */
    private LocalDate scheduleDate;

    /** 反选机台编码 */
    private String machineCode;

    /** 检测时机台在机物料（前物料），仅用于日志对账 */
    private String previousMaterialCode;

    /** 反选命中物料编码 */
    private String materialCode;

    /** 反选命中物料产品状态 */
    private String productStatus;

    /** 反选命中物料在当天 S4.5 排序中的全局名次，用于稳定排序与日志对账 */
    private Integer skuSortRank;

    /** 换活字块匹配层级说明，当前统一为“同胎胚+同模具” */
    private String matchedLayer;

    /** 是否已在当天正常资源竞争阶段尝试 */
    private boolean attempted;

    /** 是否已由新增主链在预留机台上成功落地 */
    private boolean success;

    /** 是否已满足（成功落地或由同物料前序结果满足） */
    private boolean satisfied;

    /** 成功说明或明确失败原因 */
    private String resultReason;
}
