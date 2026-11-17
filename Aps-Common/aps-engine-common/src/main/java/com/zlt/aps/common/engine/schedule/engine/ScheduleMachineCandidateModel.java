package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** TM/TC 自动排程候选机台公共运行态模型。 */
@Data
public class ScheduleMachineCandidateModel implements ScheduleQualityMachineCandidate {

    /**
     * 机台编码。
     */
    protected String machineCode;
    /** 机台是否启用。 */
    protected Boolean enabled;
    /** 机台开放班次编码集合（如早/中/夜班次编码）。 */
    protected Set<String> openShiftCodes;
    /** 机台当班最大产能（米）。 */
    protected BigDecimal maxCapacity;
    /** 机台当班剩余可用产能（米）。 */
    protected BigDecimal remainCapacity;
    /** 机台默认检修/停机时长（小时）。 */
    protected BigDecimal maintenanceHours;
    /** 按排程班次序号记录的机台检修/停机时长（小时）。 */
    protected Map<Integer, BigDecimal> maintenanceHoursByShift = new LinkedHashMap<>();
    /** 机台换算后的默认生产速度（米/小时）。 */
    protected BigDecimal machineSpeed;
    /**
     * 机台默认速度资料原值，米/分钟；不得参与内部耗时计算。
     */
    protected BigDecimal machineSpeedRawValue;
    /** 产品规格对应的机台生产速度。TM/TC 运行态统一使用 processCode 作为键。 */
    protected Map<String, BigDecimal> processSpeedMap = new LinkedHashMap<>();
    /**
     * 规格速度资料原值，米/分钟，仅用于解释换算依据。
     */
    protected Map<String, BigDecimal> processSpeedRawMap = new LinkedHashMap<>();
    /**
     * 机台可支持的口型板(口型板配置)
     */
    protected Set<String> configuredMouthPlateCodes;
    /** 当前机台实际可用的口型板编码集合。 */
    protected Set<String> mouthPlateCodes;
    /**
     * 机台支持的胶料(胶料机台关系配置)
     */
    protected Set<String> configuredGlueCodes;
    /** 当前机台允许生产的胶料编码集合。 */
    protected Set<String> allowedGlueCodes;
    /** 当前机台禁止生产的胶料编码集合。 */
    protected Set<String> forbiddenGlueCodes;
    /** 是否通过口型板匹配校验。 */
    protected Boolean mouthPlateMatched;
    /** 是否通过胶料机台匹配校验。 */
    protected Boolean glueMachineMatched;
    /** 是否命中定点机台优选。 */
    protected Boolean fixedMachineSelected;
    /** 是否命中禁排机台排除。 */
    protected Boolean fixedMachineExcluded;
    /** 工厂已配置的定点允许产品集合(有配置的定点机台) */
    protected Set<String> configuredFixedAllowProcessCodes;
    /** 当前机台可定点生产的产品集合(定点机台对应产品规格) */
    protected Set<String> fixedAllowProcessCodes;
    /** 当前机台禁止定点生产的产品集合(禁排机台对应产品规格) */
    protected Set<String> fixedForbidProcessCodes;
    /** 机台当班当前链尾任务的主胶料编码。 */
    protected String tailMainGlueCode;
    /** 机台当班当前链尾任务的基部胶编码。 */
    protected String tailBaseGlueCode;
    /** 机台当班当前链尾任务的口型板编码。 */
    protected String tailMouthPlateCode;
    /** 切换至当前待排任务产生的换型损失时长（小时）。 */
    protected BigDecimal switchCostHours;
    /** 是否通过定点/禁排机台规则匹配校验。 */
    protected Boolean fixedMachineMatched;
    /** 当前候选机台是否已被过滤淘汰。 */
    protected Boolean filtered = Boolean.FALSE;
    /** 候选机台被过滤淘汰的原因编码。 */
    protected String filterReasonCode;
    /** 候选机台被过滤淘汰的原因描述。 */
    protected String filterReasonDesc;
    /** 选机过滤与评分过程的解释证据映射。 */
    protected Map<String, Object> evidence = new LinkedHashMap<>();
    /** 候选机台综合评分总分。 */
    protected BigDecimal score = BigDecimal.ZERO;
    /** 候选机台各维度评分明细结果。 */
    protected ScheduleScoreResult scoreResult;

    /**
     * 复制机台候选及其可变过滤、评分容器，供隔离备库试算使用。
     *
     * @return 不共享集合状态的候选副本
     */
    public ScheduleMachineCandidateModel copyForWorkspace() {
        ScheduleMachineCandidateModel copy = new ScheduleMachineCandidateModel();
        this.copyBaseWorkspaceFieldsTo(copy);
        return copy;
    }

    /**
     * 复制公共候选字段到领域候选副本。
     *
     * @param copy 已创建的目标候选
     */
    protected void copyBaseWorkspaceFieldsTo(ScheduleMachineCandidateModel copy) {
        BeanUtils.copyProperties(this, copy);
        copy.setOpenShiftCodes(ScheduleWorkspaceCopyUtils.copySet(this.openShiftCodes));
        copy.setMaintenanceHoursByShift(ScheduleWorkspaceCopyUtils.copyMap(this.maintenanceHoursByShift));
        copy.setProcessSpeedMap(ScheduleWorkspaceCopyUtils.copyMap(this.processSpeedMap));
        copy.setProcessSpeedRawMap(ScheduleWorkspaceCopyUtils.copyMap(this.processSpeedRawMap));
        copy.setConfiguredMouthPlateCodes(ScheduleWorkspaceCopyUtils.copySet(this.configuredMouthPlateCodes));
        copy.setMouthPlateCodes(ScheduleWorkspaceCopyUtils.copySet(this.mouthPlateCodes));
        copy.setConfiguredGlueCodes(ScheduleWorkspaceCopyUtils.copySet(this.configuredGlueCodes));
        copy.setAllowedGlueCodes(ScheduleWorkspaceCopyUtils.copySet(this.allowedGlueCodes));
        copy.setForbiddenGlueCodes(ScheduleWorkspaceCopyUtils.copySet(this.forbiddenGlueCodes));
        copy.setConfiguredFixedAllowProcessCodes(
                ScheduleWorkspaceCopyUtils.copySet(this.configuredFixedAllowProcessCodes));
        copy.setFixedAllowProcessCodes(ScheduleWorkspaceCopyUtils.copySet(this.fixedAllowProcessCodes));
        copy.setFixedForbidProcessCodes(ScheduleWorkspaceCopyUtils.copySet(this.fixedForbidProcessCodes));
        copy.setEvidence(ScheduleWorkspaceCopyUtils.copyEvidenceMap(this.evidence));
        copy.setScoreResult(ScheduleWorkspaceCopyUtils.copyBean(this.scoreResult, ScheduleScoreResult.class));
    }

    /**
     * 判断当前候选机台是否已被规则过滤淘汰。
     *
     * @return 已过滤返回 true，否则返回 false
     */
    public boolean isFiltered() {
        return filtered != null && filtered;
    }

    /**
     * 获取候选机台的过滤与评分证据映射。
     *
     * @return 证据映射
     */
    public Map<String, Object> getFilterEvidence() {
        return evidence;
    }

    /**
     * 标记候选机台被过滤淘汰，并追加结构化证据键值对。
     *
     * @param reasonCode   过滤原因编码
     * @param reasonDesc   过滤原因描述
     * @param evidenceData 补充证据键值对
     */
    public void markFiltered(String reasonCode, String reasonDesc, Map<String, String> evidenceData) {
        this.filtered = Boolean.TRUE;
        this.filterReasonCode = reasonCode;
        this.filterReasonDesc = reasonDesc;
        if (evidenceData != null) {
            this.evidence.putAll(evidenceData);
        }
    }

    /**
     * 标记候选机台被过滤淘汰，并记录单条文本证据。
     *
     * @param reasonCode   过滤原因编码
     * @param reasonDesc   过滤原因描述
     * @param evidenceData 补充证据文本
     */
    public void markFiltered(String reasonCode, String reasonDesc, String evidenceData) {
        this.filtered = Boolean.TRUE;
        this.filterReasonCode = reasonCode;
        this.filterReasonDesc = reasonDesc;
        if (evidenceData != null) {
            this.evidence.put("evidence", evidenceData);
        }
    }

    /**
     * 应用机台评分结果并写入证据快照。
     *
     * @param result 评分结果
     */
    public void applyScore(ScheduleScoreResult result) {
        if (result != null) {
            this.score = result.getTotalScore();
            this.scoreResult = result;
            this.evidence.put("scoreResult", result);
        }
    }
}
