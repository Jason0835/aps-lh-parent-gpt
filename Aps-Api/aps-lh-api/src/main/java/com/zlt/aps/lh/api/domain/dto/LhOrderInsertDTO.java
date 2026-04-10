package com.zlt.aps.lh.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/3/20
 */
@Data
public class LhOrderInsertDTO implements Serializable {

    private static final long serialVersionUID = -7701233616641916712L;

    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    private String factoryCode;

    @ApiModelProperty(value = "自动排程批次号", name = "batchNo")
    @NotBlank(message = "自动排程批次号不能为空")
    private String batchNo;

    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    @NotBlank(message = "硫化机台编号不能为空")
    private String lhMachineCode;

    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    @NotBlank(message = "硫化机台名称不能为空")
    private String lhMachineName;

    @ApiModelProperty(value = "规格代码", name = "specCode")
    @NotBlank(message = "规格代码不能为空")
    private String specCode;

    @ApiModelProperty(value = "物料编号", name = "productCode")
    @NotBlank(message = "物料编号不能为空")
    private String productCode;

    /**
     * 存储当前左右模情况，如果非单模单规格的则可为空，单模单规格则存储对应的模信息，如：存储内容，L/R、L1/R1
     */
    @ApiModelProperty(value = "左右模", name = "leftRightMold")
    private String leftRightMold;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @NotNull(message = "排程日期不能为空")
    private Date scheduleDate;

    // ==================== 一班 ====================
    /**
     * 一班计划量
     */
    @ApiModelProperty(value = "一班计划量", name = "class1PlanQty")
    @NotNull(message = "一班计划量不能为空")
    private Integer class1PlanQty;

    /**
     * 一班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "一班计划开始时间", name = "class1StartTime")
    @NotNull(message = "一班计划开始时间不能为空")
    private Date class1StartTime;

    /**
     * 一班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "一班计划结束时间", name = "class1EndTime")
    @NotNull(message = "一班计划结束时间不能为空")
    private Date class1EndTime;

    /**
     * 一班原因分析
     */
    @ApiModelProperty(value = "一班原因分析", name = "class1Analysis")
    @Excel(name = "ui.data.column.lhScheduleResult.class1Analysis")
    private String class1Analysis;

    // ==================== 二班 ====================
    /**
     * 二班计划量
     */
    @ApiModelProperty(value = "二班计划量", name = "class2PlanQty")
    @NotNull(message = "二班计划量不能为空")
    private Integer class2PlanQty;

    /**
     * 二班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "二班计划开始时间", name = "class2StartTime")
    @NotNull(message = "二班计划开始时间不能为空")
    private Date class2StartTime;

    /**
     * 二班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "二班计划结束时间", name = "class2EndTime")
    @NotNull(message = "二班计划结束时间不能为空")
    private Date class2EndTime;

    /**
     * 二班原因分析
     */
    @ApiModelProperty(value = "二班原因分析", name = "class2Analysis")
    @Excel(name = "ui.data.column.lhScheduleResult.class2Analysis")
    private String class2Analysis;

    // ==================== 三班 ====================
    /**
     * 三班计划量
     */
    @ApiModelProperty(value = "三班计划量", name = "class3PlanQty")
    @NotNull(message = "三班计划量不能为空")
    private Integer class3PlanQty;

    /**
     * 三班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "三班计划开始时间", name = "class3StartTime")
    @NotNull(message = "三班计划开始时间不能为空")
    private Date class3StartTime;

    /**
     * 三班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "三班计划结束时间", name = "class3EndTime")
    @NotNull(message = "三班计划结束时间不能为空")
    private Date class3EndTime;

    /**
     * 三班原因分析
     */
    @ApiModelProperty(value = "三班原因分析", name = "class3Analysis")
    @Excel(name = "ui.data.column.lhScheduleResult.class3Analysis")
    private String class3Analysis;

    // ==================== 四班 ====================
    /**
     * 四班计划量
     */
    @ApiModelProperty(value = "四班计划量", name = "class4PlanQty")
    private Integer class4PlanQty;

    /**
     * 四班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "四班计划开始时间", name = "class4StartTime")
    private Date class4StartTime;

    /**
     * 四班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "四班计划结束时间", name = "class4EndTime")
    private Date class4EndTime;

    /**
     * 四班原因分析
     */
    @ApiModelProperty(value = "四班原因分析", name = "class4Analysis")
    private String class4Analysis;

    // ==================== 五班 ====================
    /**
     * 五班计划量
     */
    @ApiModelProperty(value = "五班计划量", name = "class5PlanQty")
    private Integer class5PlanQty;

    /**
     * 五班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "五班计划开始时间", name = "class5StartTime")
    private Date class5StartTime;

    /**
     * 五班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "五班计划结束时间", name = "class5EndTime")
    private Date class5EndTime;

    /**
     * 五班原因分析
     */
    @ApiModelProperty(value = "五班原因分析", name = "class5Analysis")
    private String class5Analysis;

    // ==================== 六班 ====================
    /**
     * 六班计划量
     */
    @ApiModelProperty(value = "六班计划量", name = "class6PlanQty")
    private Integer class6PlanQty;

    /**
     * 六班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "六班计划开始时间", name = "class6StartTime")
    private Date class6StartTime;

    /**
     * 六班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "六班计划结束时间", name = "class6EndTime")
    private Date class6EndTime;

    /**
     * 六班原因分析
     */
    @ApiModelProperty(value = "六班原因分析", name = "class6Analysis")
    private String class6Analysis;

    // ==================== 新增 七班 ====================
    /**
     * 七班计划量
     */
    @ApiModelProperty(value = "七班计划量", name = "class7PlanQty")
    private Integer class7PlanQty;

    /**
     * 七班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "七班计划开始时间", name = "class7StartTime")
    private Date class7StartTime;

    /**
     * 七班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "七班计划结束时间", name = "class7EndTime")
    private Date class7EndTime;

    /**
     * 七班原因分析
     */
    @ApiModelProperty(value = "七班原因分析", name = "class7Analysis")
    private String class7Analysis;

    // ==================== 新增 八班 ====================
    /**
     * 八班计划量
     */
    @ApiModelProperty(value = "八班计划量", name = "class8PlanQty")
    private Integer class8PlanQty;

    /**
     * 八班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "八班计划开始时间", name = "class8StartTime")
    private Date class8StartTime;

    /**
     * 八班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "八班计划结束时间", name = "class8EndTime")
    private Date class8EndTime;

    /**
     * 八班原因分析
     */
    @ApiModelProperty(value = "八班原因分析", name = "class8Analysis")
    private String class8Analysis;

    // ==================== 其他字段 ====================
    @ApiModelProperty(value = "是否交期")
    private String isDelivery;

}