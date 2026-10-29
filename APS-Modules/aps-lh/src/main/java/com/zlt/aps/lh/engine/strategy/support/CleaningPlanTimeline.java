package com.zlt.aps.lh.engine.strategy.support;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import java.util.Date;
import com.zlt.aps.lh.api.enums.CleaningTypeEnum;
/**
 * 清洗纯时间结果，来源、清洗、首检和机台释放分别表达，不参与资源分配或数量扣账。
 *
 * @author APS
 * @since 2026/10/19
 */
public final class CleaningPlanTimeline {
    /**
     * 来源计划开始时间。
     */
    private final Date sourcePlanStartTime;
    /**
     * 来源计划结束时间。
     */
    private final Date sourcePlanEndTime;
    /**
     * 实际清洗开始时间。
     */
    private final Date cleanStartTime;
    /**
     * 实际清洗结束时间，不随维修、精度及首检等待后移。
     */
    private final Date cleanEndTime;
    /**
     * 已显式安排的在机喷砂首检开始时间，未组合时为空。
     */
    private final Date inspectionStartTime;
    /**
     * 普通在机喷砂首检结束时间；离机喷砂不承接机台首检。
     */
    private final Date inspectionEndTime;
    /**
     * 机台恢复时间；置换含准备和首检，无替换模具的离机清洗在清洗开始时释放。
     */
    private final Date productionResumeTime;
    /**
     * 新模具置换首检开始时间，离机喷砂首检不复用到新模具。
     */
    private final Date replacementInspectionStartTime;
    /**
     * 新模具置换首检完成及正常生产恢复时间。
     */
    private final Date replacementReadyTime;
    /**
     * 从当前清洗事实冻结各时间边界。
     *
     * @param window 清洗事实窗口
     */
    public CleaningPlanTimeline(MachineCleaningWindowDTO window) {
        this.sourcePlanStartTime = copy(window.getSourcePlanStartTime());
        this.sourcePlanEndTime = copy(window.getSourcePlanEndTime());
        this.cleanStartTime = copy(window.getCleanStartTime());
        this.cleanEndTime = copy(window.getCleanEndTime());
        this.inspectionStartTime = copy(window.getSandBlastInspectionStartTime());
        this.inspectionEndTime = !window.isOffMachineCleaning()
                && CleaningTypeEnum.SAND_BLAST.getCode().equals(window.getCleanType())
                ? copy(window.getReadyTime()) : null;
        this.replacementInspectionStartTime = copy(window.getReplacementInspectionStartTime());
        this.replacementReadyTime = copy(window.getReplacementReadyTime());
        Date resumeTime = window.getReadyTime();
        if (window.isOffMachineCleaning()) {
            resumeTime = replacementReadyTime == null ? window.getCleanStartTime() : replacementReadyTime;
        }
        this.productionResumeTime = copy(resumeTime);
    }
    /**
     * 获取来源计划开始时间的独立副本。
     *
     * @return 时间副本，未形成该时间时为空
     */
    public Date getSourcePlanStartTime() {
        return copy(sourcePlanStartTime);
    }
    /**
     * 获取来源计划结束时间的独立副本。
     *
     * @return 时间副本，未形成该时间时为空
     */
    public Date getSourcePlanEndTime() {
        return copy(sourcePlanEndTime);
    }
    /**
     * 获取实际清洗开始时间的独立副本。
     *
     * @return 时间副本，未形成该时间时为空
     */
    public Date getCleanStartTime() {
        return copy(cleanStartTime);
    }
    /**
     * 获取实际清洗结束时间的独立副本。
     *
     * @return 时间副本，未形成该时间时为空
     */
    public Date getCleanEndTime() {
        return copy(cleanEndTime);
    }
    /**
     * 获取显式安排的在机喷砂首检开始时间。
     *
     * @return 独立日期副本，普通未组合喷砂为空
     */
    public Date getInspectionStartTime() {
        return copy(inspectionStartTime);
    }
    /**
     * 获取普通在机喷砂首检结束时间；离机喷砂不承接机台首检的独立副本。
     *
     * @return 时间副本，未形成该时间时为空
     */
    public Date getInspectionEndTime() {
        return copy(inspectionEndTime);
    }
    /**
     * 获取机台恢复时间；置换含准备和首检，无替换模具的离机清洗在清洗开始时释放的独立副本。
     *
     * @return 时间副本，未形成该时间时为空
     */
    public Date getProductionResumeTime() {
        return copy(productionResumeTime);
    }
    /**
     * 获取冻结的新模具首检开始时间。
     *
     * @return 独立时间副本，尚未落实置换时为空
     */
    public Date getReplacementInspectionStartTime() {
        return copy(replacementInspectionStartTime);
    }
    /**
     * 获取冻结的新模具首检结束时间。
     *
     * @return 独立时间副本，尚未落实置换时为空
     */
    public Date getReplacementReadyTime() {
        return copy(replacementReadyTime);
    }
    /**
     * 将置换机台准备和首检投影为公共容量内核输入，保留原清洗事实不变。
     *
     * @param window 已冻结的离机喷砂置换事件
     * @param includeInspection 当前物料和状态是否可以承接本次置换首检
     * @return 独立容量窗口；无置换时间结果时为空
     */
    public MachineCleaningWindowDTO toReplacementCapacityWindow(MachineCleaningWindowDTO window,
            boolean includeInspection) {
        if (!window.isOffMachineCleaning() || replacementInspectionStartTime == null
                || replacementReadyTime == null || window.getReplacementMouldCode() == null) {
            return null;
        }
        // 复用已有首检计量及时间推进算法；本投影不进入清洗计划、配额或来源回填集合。
        MachineCleaningWindowDTO capacity = new MachineCleaningWindowDTO();
        capacity.setSourcePlanId(window.getSourcePlanId());
        capacity.setLhCode(window.getLhCode());
        capacity.setCleanType(CleaningTypeEnum.SAND_BLAST.getCode());
        capacity.setOffMachineCleaning(true);
        capacity.setReplacementMouldCode(window.getReplacementMouldCode());
        capacity.setCleanStartTime(copy(cleanStartTime));
        capacity.setCleanEndTime(copy(replacementInspectionStartTime));
        capacity.setReadyTime(copy(replacementReadyTime));
        capacity.setSandBlastFirstInspectionQty(includeInspection ? window.getReplacementFirstInspectionQty() : 0);
        capacity.setSandBlastInspectionShiftStartTime(copy(window.getReplacementInspectionShiftStartTime()));
        return capacity;
    }
    /**
     * 复制日期，避免候选调用方修改已冻结的时间。
     *
     * @param time 原时间
     * @return 日期副本
     */
    private static Date copy(Date time) {
        return time == null ? null : (Date) time.clone();
    }
}
