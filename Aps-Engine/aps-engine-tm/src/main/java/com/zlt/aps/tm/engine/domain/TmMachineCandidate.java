package com.zlt.aps.tm.engine.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

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

    /** 剩余产能，单位米 */
    private BigDecimal remainCapacity;

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
}
