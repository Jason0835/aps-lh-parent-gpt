package com.zlt.aps.tm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎面排程结果 Excel 明细行对象。
 */
@Data
public class TmScheduleResultExcelRow {

    /** Excel 行号，从 1 开始。 */
    private Integer rowNum;

    /** 胎面编码。 */
    private String treadCode;

    /** 胎面长度。 */
    private BigDecimal treadLength;

    /** 成型余量。 */
    private BigDecimal cxRemainQty;

    /** 物料编码。 */
    private String materialCode;

    /** 物料描述。 */
    private String materialDesc;

    /** 整条胶料组合编码。 */
    private String wholeGlueCode;

    /** 6 点库存。 */
    private BigDecimal stockQty;

    /** 1 班计划量。 */
    private BigDecimal class1PlanQty;

    /** 2 班计划量。 */
    private BigDecimal class2PlanQty;

    /** 3 班计划量。 */
    private BigDecimal class3PlanQty;

    /** 卷曲长度。 */
    private BigDecimal curlRollLength;

    /** 成型机台编码。 */
    private String cxMachineCode;

    /** 胎面机台编码，来自隐藏列。 */
    private String machineCode;

    /** 1 班顺序，来自隐藏列。 */
    private Integer class1Sequence;

    /** 2 班顺序，来自隐藏列。 */
    private Integer class2Sequence;

    /** 3 班顺序，来自隐藏列。 */
    private Integer class3Sequence;

    /** 当前行解析和业务校验错误。 */
    private List<String> errors = new ArrayList<>();
}
