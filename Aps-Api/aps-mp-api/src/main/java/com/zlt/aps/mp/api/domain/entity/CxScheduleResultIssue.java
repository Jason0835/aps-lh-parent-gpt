package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成型排程结果下发对象
 * 对应中间表：MES_CX_SCHEDULE_RESULT
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "成型排程结果下发对象", description = "成型排程结果下发对象")
public class CxScheduleResultIssue extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 成型批次号
     */
    @ApiModelProperty(value = "成型批次号", name = "cxBatchNo")
    private String cxBatchNo;

    /**
     * 工单号
     */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    private String orderNo;

    /**
     * 排程日期
     */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private LocalDateTime scheduleDate;

    /**
     * 成型机台编号
     */
    @ApiModelProperty(value = "成型机台编号", name = "machineCode")
    private String machineCode;

    /**
     * 成型机台名称
     */
    @ApiModelProperty(value = "成型机台名称", name = "machineName")
    private String machineName;

    /**
     * 硫化机台编号
     */
    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    private String lhMachineCode;

    /**
     * 硫化机台名称
     */
    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    private String lhMachineName;

    /**
     * 可用模具数量
     */
    @ApiModelProperty(value = "可用模具数量", name = "availableMoldQty")
    private BigDecimal availableMoldQty;

    /**
     * 物料编码（NC）
     */
    @ApiModelProperty(value = "物料编码（NC）", name = "materialCode")
    private String materialCode;

    /**
     * 物料编码（MES）
     */
    @ApiModelProperty(value = "物料编码（MES）", name = "mesMaterialCode")
    private String mesMaterialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "specDesc")
    private String specDesc;

    /**
     * 成型胎胚物料编码
     */
    @ApiModelProperty(value = "成型胎胚物料编码", name = "embryoCode")
    private String embryoCode;

    /**
     * 成型胎胚物料描述
     */
    @ApiModelProperty(value = "成型胎胚物料描述", name = "embryoSpecDesc")
    private String embryoSpecDesc;

    // ========== 一班（夜班） ==========
    /**
     * 一班顺序
     */
    @ApiModelProperty(value = "一班顺序", name = "class1PlanQtySeq")
    private BigDecimal class1PlanQtySeq;

    /**
     * 夜班原因分析手工输入
     */
    @ApiModelProperty(value = "夜班原因分析手工输入", name = "class1AnalysisInput")
    private String class1AnalysisInput;

    /**
     * 夜班原因分析
     */
    @ApiModelProperty(value = "夜班原因分析", name = "class1Analysis")
    private String class1Analysis;

    /**
     * 夜班计划数
     */
    @ApiModelProperty(value = "夜班计划数", name = "class1PlanQty")
    private BigDecimal class1PlanQty;

    /**
     * 夜班示方类型
     */
    @ApiModelProperty(value = "夜班示方类型", name = "class1ExampleType")
    private String class1ExampleType;

    /**
     * 夜班示方号
     */
    @ApiModelProperty(value = "夜班示方号", name = "class1ExampleNo")
    private String class1ExampleNo;

    // ========== 二班（早班） ==========
    /**
     * 二班顺序
     */
    @ApiModelProperty(value = "二班顺序", name = "class2PlanQtySeq")
    private BigDecimal class2PlanQtySeq;

    /**
     * 早班原因分析手工输入
     */
    @ApiModelProperty(value = "早班原因分析手工输入", name = "class2AnalysisInput")
    private String class2AnalysisInput;

    /**
     * 早班原因分析
     */
    @ApiModelProperty(value = "早班原因分析", name = "class2Analysis")
    private String class2Analysis;

    /**
     * 早班计划数
     */
    @ApiModelProperty(value = "早班计划数", name = "class2PlanQty")
    private BigDecimal class2PlanQty;

    /**
     * 早班示方类型
     */
    @ApiModelProperty(value = "早班示方类型", name = "class2ExampleType")
    private String class2ExampleType;

    /**
     * 早班示方号
     */
    @ApiModelProperty(value = "早班示方号", name = "class2ExampleNo")
    private String class2ExampleNo;

    // ========== 三班（中班） ==========
    /**
     * 三班顺序
     */
    @ApiModelProperty(value = "三班顺序", name = "class3PlanQtySeq")
    private BigDecimal class3PlanQtySeq;

    /**
     * 中班原因分析手工输入
     */
    @ApiModelProperty(value = "中班原因分析手工输入", name = "class3AnalysisInput")
    private String class3AnalysisInput;

    /**
     * 中班原因分析
     */
    @ApiModelProperty(value = "中班原因分析", name = "class3Analysis")
    private String class3Analysis;

    /**
     * 中班计划数
     */
    @ApiModelProperty(value = "中班计划数", name = "class3PlanQty")
    private BigDecimal class3PlanQty;

    /**
     * 中班示方类型
     */
    @ApiModelProperty(value = "中班示方类型", name = "class3ExampleType")
    private String class3ExampleType;

    /**
     * 中班示方号
     */
    @ApiModelProperty(value = "中班示方号", name = "class3ExampleNo")
    private String class3ExampleNo;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    private String dataVersion;

    /**
     * 分公司编码
     */
    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    private String companyCode;

    /**
     * 厂别
     */
    @ApiModelProperty(value = "厂别", name = "factoryCode")
    private String factoryCode;
}
