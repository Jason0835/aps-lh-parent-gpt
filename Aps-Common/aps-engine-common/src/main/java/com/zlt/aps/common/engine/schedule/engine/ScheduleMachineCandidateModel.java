package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import lombok.Data;

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
    /** 产品规格对应的机台生产速度。TM/TC 运行态统一使用 processCode 作为键。 */
    protected Map<String, BigDecimal> processSpeedMap = new LinkedHashMap<>();
    protected Set<String> configuredMouthPlateCodes;
    protected Set<String> mouthPlateCodes;
    protected Set<String> configuredGlueCodes;
    protected Set<String> allowedGlueCodes;
    protected Set<String> forbiddenGlueCodes;
    protected Boolean mouthPlateMatched;
    protected Boolean glueMachineMatched;
    protected Boolean fixedMachineSelected;
    protected Boolean fixedMachineExcluded;
    /** 工厂已配置的定点允许产品集合。 */
    protected Set<String> configuredFixedAllowProcessCodes;
    /** 当前机台可定点生产的产品集合。 */
    protected Set<String> fixedAllowProcessCodes;
    /** 当前机台禁止定点生产的产品集合。 */
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
