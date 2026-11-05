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

    protected String machineCode;
    protected Boolean enabled;
    protected Set<String> openShiftCodes;
    protected BigDecimal maxCapacity;
    protected BigDecimal remainCapacity;
    protected BigDecimal maintenanceHours;
    protected Map<Integer, BigDecimal> maintenanceHoursByShift = new LinkedHashMap<>();
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
    protected Set<String> mouthPlateCodes;
    /**
     * 机台支持的胶料(胶料机台关系配置)
     */
    protected Set<String> configuredGlueCodes;
    protected Set<String> allowedGlueCodes;
    protected Set<String> forbiddenGlueCodes;
    protected Boolean mouthPlateMatched;
    protected Boolean glueMachineMatched;
    protected Boolean fixedMachineSelected;
    protected Boolean fixedMachineExcluded;
    /** 工厂已配置的定点允许产品集合(有配置的定点机台) */
    protected Set<String> configuredFixedAllowProcessCodes;
    /** 当前机台可定点生产的产品集合(定点机台对应产品规格) */
    protected Set<String> fixedAllowProcessCodes;
    /** 当前机台禁止定点生产的产品集合(禁排机台对应产品规格) */
    protected Set<String> fixedForbidProcessCodes;
    protected String tailMainGlueCode;
    protected String tailBaseGlueCode;
    protected String tailMouthPlateCode;
    protected BigDecimal switchCostHours;
    protected Boolean fixedMachineMatched;
    protected Boolean filtered = Boolean.FALSE;
    protected String filterReasonCode;
    protected String filterReasonDesc;
    protected Map<String, Object> evidence = new LinkedHashMap<>();
    protected BigDecimal score = BigDecimal.ZERO;
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

    public boolean isFiltered() {
        return filtered != null && filtered;
    }

    public Map<String, Object> getFilterEvidence() {
        return evidence;
    }

    public void markFiltered(String reasonCode, String reasonDesc, Map<String, String> evidenceData) {
        this.filtered = Boolean.TRUE;
        this.filterReasonCode = reasonCode;
        this.filterReasonDesc = reasonDesc;
        if (evidenceData != null) {
            this.evidence.putAll(evidenceData);
        }
    }

    public void markFiltered(String reasonCode, String reasonDesc, String evidenceData) {
        this.filtered = Boolean.TRUE;
        this.filterReasonCode = reasonCode;
        this.filterReasonDesc = reasonDesc;
        if (evidenceData != null) {
            this.evidence.put("evidence", evidenceData);
        }
    }

    public void applyScore(ScheduleScoreResult result) {
        if (result != null) {
            this.score = result.getTotalScore();
            this.scoreResult = result;
            this.evidence.put("scoreResult", result);
        }
    }
}
