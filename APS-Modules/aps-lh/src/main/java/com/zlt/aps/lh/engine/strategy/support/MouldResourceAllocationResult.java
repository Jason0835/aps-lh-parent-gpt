package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.Set;

/**
 * 增机台模具资源分配结果。
 *
 * @author APS
 */
@Data
public class MouldResourceAllocationResult {

    /** 是否允许增加当前候选机台 */
    private boolean allowed;
    /** 当前候选机台编码 */
    private String machineCode;
    /** 当前候选机台所需模具数量 */
    private int requiredMouldQty;
    /** SKU台账启用的可用模具总数 */
    private int availableMouldQty;
    /** SKU已被新增链路占用的模具数量 */
    private int occupiedMouldQty;
    /** 当前分配前剩余可用模具数量 */
    private int remainingAvailableMouldQty;
    /** 本次分配的模具号 */
    private List<String> allocatedMouldCodeList = new ArrayList<String>(0);
    /** 本次真实换模释放的前物料模具号 */
    private List<String> releasedMouldCodeList = new ArrayList<String>(0);
    /** 当前SKU因其他SKU占用不可选的模具号 */
    private List<String> occupiedMouldCodeList = new ArrayList<String>(0);
    /** 当前SKU台账缺失或禁用的模具号 */
    private List<String> unavailableMouldCodeList = new ArrayList<String>(0);
    /** 不允许增加时的跳过原因 */
    private MouldResourceSkipReason skipReason;
    /**
     * 原分配请求的资源时刻，不等同下一次调度日期。
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Date resourceReferenceTime;
    /**
     * 已提交释放事实能够证明的最早解除边界；无充分依据时为空。
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Date shortageBlockedUntil;
    /**
     * 只读精确子因，不替换原 allowed 或 skipReason。
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<MouldResourceDiagnosticCause> diagnosticCauses = Collections.emptySet();
    /**
     * 原返回值是否明确证明数量不足；关系、台账及指定组合异常不单独作为数量证据。
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private boolean quantityShortage;

    /**
     * 构建允许分配结果。
     *
     * @param requiredMouldQty 当前候选机台所需模具数量
     * @param availableMouldQty 可用模具总数
     * @param occupiedMouldQty 已占用模具数量
     * @param remainingAvailableMouldQty 分配后剩余可用模具数量
     * @param allocatedMouldCodeList 本次分配模具号
     * @param releasedMouldCodeList 本次释放模具号
     * @return 分配结果
     */
    public static MouldResourceAllocationResult allowed(int requiredMouldQty,
                                                        int availableMouldQty,
                                                        int occupiedMouldQty,
                                                        int remainingAvailableMouldQty,
                                                        List<String> allocatedMouldCodeList,
                                                        List<String> releasedMouldCodeList) {
        MouldResourceAllocationResult result = new MouldResourceAllocationResult();
        result.setAllowed(true);
        result.setRequiredMouldQty(requiredMouldQty);
        result.setAvailableMouldQty(availableMouldQty);
        result.setOccupiedMouldQty(occupiedMouldQty);
        result.setRemainingAvailableMouldQty(remainingAvailableMouldQty);
        result.setAllocatedMouldCodeList(allocatedMouldCodeList == null
                ? Collections.<String>emptyList() : new ArrayList<String>(allocatedMouldCodeList));
        result.setReleasedMouldCodeList(releasedMouldCodeList == null
                ? Collections.<String>emptyList() : new ArrayList<String>(releasedMouldCodeList));
        return result;
    }

    /**
     * 构建拒绝分配结果。
     *
     * @param requiredMouldQty 当前候选机台所需模具数量
     * @param availableMouldQty 可用模具总数
     * @param occupiedMouldQty 已占用模具数量
     * @param remainingAvailableMouldQty 剩余可用模具数量
     * @param occupiedMouldCodeList 已占用模具号
     * @param unavailableMouldCodeList 台账不可用模具号
     * @param skipReason 跳过原因
     * @return 分配结果
     */
    public static MouldResourceAllocationResult rejected(int requiredMouldQty,
                                                         int availableMouldQty,
                                                         int occupiedMouldQty,
                                                         int remainingAvailableMouldQty,
                                                         List<String> occupiedMouldCodeList,
                                                         List<String> unavailableMouldCodeList,
                                                         MouldResourceSkipReason skipReason) {
        MouldResourceAllocationResult result = new MouldResourceAllocationResult();
        result.setAllowed(false);
        result.setRequiredMouldQty(requiredMouldQty);
        result.setAvailableMouldQty(availableMouldQty);
        result.setOccupiedMouldQty(occupiedMouldQty);
        result.setRemainingAvailableMouldQty(remainingAvailableMouldQty);
        result.setAllocatedMouldCodeList(Collections.<String>emptyList());
        result.setOccupiedMouldCodeList(occupiedMouldCodeList == null
                ? Collections.<String>emptyList() : new ArrayList<String>(occupiedMouldCodeList));
        result.setUnavailableMouldCodeList(unavailableMouldCodeList == null
                ? Collections.<String>emptyList() : new ArrayList<String>(unavailableMouldCodeList));
        result.setSkipReason(skipReason);
        return result;
    }
}
