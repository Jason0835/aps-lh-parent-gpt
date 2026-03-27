package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 硫化排程结果下发实体
 * 用于将硫化排程结果下发到MES系统
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
public class LhScheduleResultIssue {

    /** 主键ID */
    private Long id;

    /** 硫化批次号 */
    private String lhBatchNo;

    /** 工单号 */
    private String orderNo;

    /** 排程日期 */
    private LocalDateTime scheduleDate;

    /** 硫化机台编码 */
    private String lhMachineCode;

    /** 硫化机台名称 */
    private String lhMachineName;

    /** 左右模 */
    private String leftRightMold;

    /** 物料编码 */
    private String materialCode;

    /** MES物料编码 */
    private String mesMaterialCode;

    /** 规格代码 */
    private String specCode;

    /** 规格描述 */
    private String specDesc;

    /** 日计划数量 */
    private Integer dailyPlanQty;

    /** 1班计划数量序号 */
    private Integer class1PlanQtySeq;

    /** 1班分析投入 */
    private String class1AnalysisInput;

    /** 1班分析 */
    private String class1Analysis;

    /** 1班计划数量 */
    private Integer class1PlanQty;

    /** 1班示例类型 */
    private String class1ExampleType;

    /** 1班示例编号 */
    private String class1ExampleNo;

    /** 2班计划数量序号 */
    private Integer class2PlanQtySeq;

    /** 2班分析投入 */
    private String class2AnalysisInput;

    /** 2班分析 */
    private String class2Analysis;

    /** 2班计划数量 */
    private Integer class2PlanQty;

    /** 2班示例类型 */
    private String class2ExampleType;

    /** 2班示例编号 */
    private String class2ExampleNo;

    /** 3班计划数量序号 */
    private Integer class3PlanQtySeq;

    /** 3班分析投入 */
    private String class3AnalysisInput;

    /** 3班分析 */
    private String class3Analysis;

    /** 3班计划数量 */
    private Integer class3PlanQty;

    /** 3班示例类型 */
    private String class3ExampleType;

    /** 3班示例编号 */
    private String class3ExampleNo;

    /** 硫化时长 */
    private Integer lhTime;

    /** 数据版本 */
    private String dataVersion;

    /** 公司代码 */
    private String companyCode;

    /** 工厂代码 */
    private String factoryCode;
}
