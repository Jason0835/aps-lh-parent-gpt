package com.zlt.aps.lh.component;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 结构可收尾时间重算摘要（{@link StructureEndingRecalcService#recalc} 返回值）。
 *
 * <p>用于日志之外的调用方判断与现场排查：哪些结构被重算、余量与组班产各是多少、
 * 哪些结构因余量为 0 / 班产为 0 / 超出预测窗口而跳过。
 *
 * @author APS
 */
@Data
public class StructureEndingRecalcSummary {

    /** 分厂编码 */
    private String factoryCode;

    /** 排程日（T_CX_EMBRYO_LH_TIME.SCHEDULE_DATE 批次） */
    private Date scheduleDate;

    /** 参考时刻（余量时点与班次外推起点） */
    private Date referenceTime;

    /** 本批次涉及的结构总数（切换记录里去重后的前结构数） */
    private int totalStructureCount;

    /** 实际更新行数 */
    private int updatedRowCount;

    /** 走日历锚点路径（降机台 / 结构交换 / 转产降机台）重算的结构数 */
    private int calendarAnchoredCount;

    /** 非主销且余量 ≤ 收尾舍弃阈值、按 0 计而未参与推算的胎胚数 */
    private int discardedNonMainCount;

    /** 重算成功明细 */
    private List<StructureRecalcDetail> recalculatedDetails = new ArrayList<>();

    /** 成型余量已耗尽（≤ 0）而跳过的结构 */
    private Set<String> skippedRemainderZeroStructures = new LinkedHashSet<>();

    /** 结构组班产为 0（无可用机台 / 无配比 / 无日硫化量）而跳过的结构 */
    private Set<String> skippedCapacityZeroStructures = new LinkedHashSet<>();

    /** 还需班次超过预测窗口（MAX_PREDICT_SHIFTS）而跳过的结构 */
    private Set<String> skippedOverLimitStructures = new LinkedHashSet<>();

    /**
     * 单结构重算明细。
     */
    @Data
    public static class StructureRecalcDetail {

        /** 前结构名 */
        private String structureName;

        /** 重算用成型余量（条） */
        private int remainder;

        /** 重算用结构组班产（条/班） */
        private int shiftCapacity;

        /** 还需班次（⌈余量 ÷ 组班产⌉） */
        private int extraShifts;

        /** 重算后的收尾时刻 */
        private Date endingTime;

        /** 重算路径：日历锚点原因 或「余量推算」 */
        private String recalcPath;

        StructureRecalcDetail(String structureName, int remainder, int shiftCapacity,
                              int extraShifts, Date endingTime, String recalcPath) {
            this.structureName = structureName;
            this.remainder = remainder;
            this.shiftCapacity = shiftCapacity;
            this.extraShifts = extraShifts;
            this.endingTime = endingTime;
            this.recalcPath = recalcPath;
        }
    }
}
