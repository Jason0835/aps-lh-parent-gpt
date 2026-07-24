package com.zlt.aps.tm.engine.domain;

import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 胎面机台候选对象。
 *
 * <p>用于承载机台过滤、评分输入和结果。过滤、评分方法会修改本对象的过滤状态和评分结果，
 * 不修改任务链。</p>
 */
@Data
public class TmMachineCandidate {

    /** 机台编码 */
    private String machineCode;

    /** 是否启用 */
    private Boolean enabled;

    /** 最大产能，单位米 */
    private BigDecimal maxCapacity;

    /** 剩余产能，单位米 */
    private BigDecimal remainCapacity;

    /** 检修时长，单位小时 */
    private BigDecimal maintenanceHours;

    /** 按班次分摊的检修时长，key 为班次顺序，value 为检修小时数 */
    private Map<Integer, BigDecimal> maintenanceHoursByShift = new LinkedHashMap<>();

    /** 机台默认生产速度，单位米/小时 */
    private BigDecimal machineSpeed;

    /** 胎面规格生产速度映射，key 为胎面编码 */
    private Map<String, BigDecimal> treadSpeedMap = new LinkedHashMap<>();

    /** 当前工厂候选机台已配置的口型板集合，用于判断未配置口型板是否不限制机台 */
    private Set<String> configuredMouthPlateCodes;

    /** 当前机台可用口型板集合 */
    private Set<String> mouthPlateCodes;

    /** 当前机台已配置过胶料关系的主胶料集合，用于判断未配置主胶料时是否不限制 */
    private Set<String> configuredGlueCodes;

    /** 当前机台明确允许生产的主胶料集合 */
    private Set<String> allowedGlueCodes;

    /** 当前机台明确禁止生产的主胶料集合 */
    private Set<String> forbiddenGlueCodes;

    /** 当前工厂候选机台存在定点允许配置的胎面集合，用于判断未配置胎面是否不限制机台 */
    private Set<String> configuredFixedAllowTreadCodes;

    /** 指定生产的胎面集合 */
    private Set<String> fixedAllowTreadCodes;

    /** 禁排生产的胎面集合 */
    private Set<String> fixedForbidTreadCodes;

    /** 口型板是否匹配 */
    private Boolean mouthPlateMatched;

    /** 胶料机台关系是否匹配 */
    private Boolean glueMachineMatched;

    /** 是否满足选择定点生产机台 */
    private Boolean fixedMachineSelected;

    /** 是否命中定点不可生产机台 */
    private Boolean fixedMachineExcluded;

    /** 链尾主胶料编码 */
    private String tailMainGlueCode;

    /** 链尾基部胶编码 */
    private String tailBaseGlueCode;

    /** 链尾口型板编码 */
    private String tailMouthPlateCode;

    /** 切换成本小时数 */
    private BigDecimal switchCostHours;

    /** 是否命中定点生产加分 */
    private Boolean fixedMachineMatched;

    /** 是否已被过滤 */
    private Boolean filtered = Boolean.FALSE;

    /** 过滤原因编码 */
    private String filterReasonCode;

    /** 过滤原因描述 */
    private String filterReasonDesc;

    /** 过滤或评分证据 */
    private Map<String, Object> evidence = new LinkedHashMap<>();

    /** 评分结果 */
    private BigDecimal score = BigDecimal.ZERO;

    /** 评分结果对象 */
    private ScheduleScoreResult scoreResult;

    /**
     * 获取是否已被过滤状态。
     *
     * @return true 表示已被过滤
     */
    public boolean isFiltered() {
        return filtered != null && filtered;
    }

    /**
     * 获取过滤或评分证据。
     *
     * @return 证据映射
     */
    public Map<String, Object> getFilterEvidence() {
        return evidence;
    }

    /**
     * 获取评分结果。
     *
     * @return 评分结果
     */
    public ScheduleScoreResult getScoreResult() {
        return scoreResult;
    }

    /**
     * 标记候选机台已被过滤。
     *
     * @param reasonCode 过滤原因编码
     * @param reasonDesc 过滤原因描述
     * @param evidenceData 过滤证据数据
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
     * 标记候选机台已被过滤。
     *
     * @param reasonCode 过滤原因编码
     * @param reasonDesc 过滤原因描述
     * @param evidenceData 过滤证据数据
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
     * 应用评分结果。
     *
     * @param scoreResult 评分结果
     */
    public void applyScore(ScheduleScoreResult scoreResult) {
        if (scoreResult != null) {
            this.score = scoreResult.getTotalScore();
            this.scoreResult = scoreResult;
            this.evidence.put("scoreResult", scoreResult);
        }
    }
}
