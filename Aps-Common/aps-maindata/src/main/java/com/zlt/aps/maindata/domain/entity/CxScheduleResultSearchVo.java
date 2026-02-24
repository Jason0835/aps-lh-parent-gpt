package com.zlt.aps.maindata.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 成型排程结果对象 t_cx_schedule_result
 */
@EqualsAndHashCode(callSuper = true)
@Data
// @TableName(value = "T_CX_SCHEDULE_RESULT_REF")
@TableName(value = "T_CX_SCHEDULE_RESULT")
@ApiModel(value = "成型排程结果对象", description = "成型排程结果对象 ")
public class CxScheduleResultSearchVo extends BaseEntity {

    private static final long serialVersionUID = 1L;
    private static final String BILL_CODE = "CX2025212";

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 批次号
     */
    @ApiModelProperty(value = "批次号")
    private String cxBatchNo;

    /**
     * 工单号
     */
    @ApiModelProperty(value = "工单号")
    private String orderNo;

    /**
     * 生产状态
     */
    @ApiModelProperty(value = "生产状态")
    private String productionStatus;

    /**
     * 分厂编号
     */
    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    /**
     * 排程日期
     */
    @ImportValidated(required = true, date = true)
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /**
     * 是否发布
     */
    @ApiModelProperty(value = "是否发布")
    private String isRelease;

    /**
     * 成型机台编号
     */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.cxScheduleResult.cxMachineCode")
    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    /**
     * 成型机成型法：1=一次法；2=二次法
     */
    @ApiModelProperty(value = "成型机成型法")
    private String cxMachineType;

    /**
     * 成型机台名称
     */
    @ApiModelProperty(value = "成型机台名称")
    private String cxMachineName;

    /**
     * 成型机寸口范围
     */
    @ApiModelProperty(value = "成型机寸口范围")
    private String cxMachineProRange;

    /**
     * 成型机默认定额
     */
    @ApiModelProperty(value = "成型机默认定额")
    private Integer cxMachineQty;

    /**
     * 成型机一班定额
     */
    @ApiModelProperty(value = "一班定额")
    private Double class1MachineQuota;

    /**
     * 成型机二班定额
     */
    @ApiModelProperty(value = "二班定额")
    private Double class2MachineQuota;

    /**
     * 修正后计划量
     */
    @ApiModelProperty(value = "修正后计划量")
    private Integer class2ModifyQty;

    /**
     * 修正后计划顺序
     */
    @ApiModelProperty(value = "修正后计划顺序")
    private Integer class2ModifySort;

    /**
     * 修正后计划量
     */
    @ApiModelProperty(value = "修正后计划量")
    private Integer class3ModifyQty;

    /**
     * 修正后计划顺序
     */
    @ApiModelProperty(value = "修正后计划顺序")
    private Integer class3ModifySort;

    /**
     * 成型机三班定额
     */
    @ApiModelProperty(value = "三班定额")
    private Double class3MachineQuota;

    /**
     * 成型机一班定额
     */
    @ApiModelProperty(value = "次一班定额")
    private Double class4MachineQuota;

    /**
     * 成型机二班定额
     */
    @ApiModelProperty(value = "次二班定额")
    private Double class5MachineQuota;

    /**
     * 成型机三班定额
     */
    @ApiModelProperty(value = "次三班定额")
    private Double class6MachineQuota;


    /**
     * 硫化机台编号
     */
    @ImportValidated(maxLength = 66)
    @ApiModelProperty(value = "硫化机台编号,多个/分割")
    @Excel(name = "ui.data.column.cxScheduleResult.lhMachineCode")
    private String lhMachineCode;

    /**
     * 硫化机台编号
     */
    @ApiModelProperty(value = "硫化排程ID,多个/分割")
    private String lhScheduleIds;

    /**
     * 硫化机台名称
     */
    @ApiModelProperty(value = "硫化机台名称,多个/分割")
    private String lhMachineName;

    /**
     * 灶数
     */
    @ImportValidated(number = true,min=0,max = 99)
    @Excel(name = "ui.data.column.cxScheduleResult.lhMachineQty")
    @ApiModelProperty(value = "使用模数")
    private Double lhMachineQty;

    /**
     * 硫化一班消耗
     */
    @ApiModelProperty(value = "一班消耗")
    private Double lhClass1Plan;

    /**
     * 硫化二班消耗
     */
    @ApiModelProperty(value = "二班消耗")
    private Double lhClass2Plan;

    /**
     * 硫化三班消耗
     */
    @ApiModelProperty(value = "三班消耗")
    private Double lhClass3Plan;

    /**
     * 硫化三班消耗
     */
    @ApiModelProperty(value = "次一班消耗")
    private Double lhClass4Plan;


    /**
     * 硫化三班消耗
     */
    @ApiModelProperty(value = "次二班消耗")
    private Double lhClass5Plan;


    /**
     * 硫化三班消耗
     */
    @ApiModelProperty(value = "次三班消耗")
    private Double lhClass6Plan;

    /**
     * 单胎硫化时长
     */
    @ApiModelProperty(value = "单胎硫化时长")
    private Double lhSingleTireTime;

    /**
     * 物料代码
     */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.cxScheduleResult.sapCode")
    @ApiModelProperty(value = "物料代码")
    private String sapCode;

    /**
     * 外胎代码
     */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.lhSpecifyMachine.specCode")
    @ApiModelProperty(value = "外胎代码")
    private String specCode;

    /**
     * 外胎规格型号
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.cxScheduleResult.specDesc")
    @ApiModelProperty(value = "外胎规格型号")
    private String specDesc;

    /**
     * 单班硫化量
     */
    @ApiModelProperty(value = "单班硫化量")
    private Integer singleShiftLhQty;

    /**
     * 胎胚代码
     */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.cxScheduleResult.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /**
     * 胎胚寸口
     */
    @ImportValidated(digits = true,min=0,max = 9999999)
    @Excel(name = "ui.data.column.result.specProSize")
    @ApiModelProperty(value = "胎胚寸口")
    private Double specDimension;

    /**
     * 施工版本信息
     */
    @Excel(name = "ui.data.column.productStatus.bomDataVersion")
    private  String bomDataVersion;

    /**
     * 胎胚库存
     */
    @ImportValidated(number = true,min=0,max = 99999)
    @Excel(name = "ui.data.column.result.totalStock")
    @ApiModelProperty(value = "胎胚库存")
    private Integer totalStock;

    /**
     * 成型排程计划合计
     */
    @ApiModelProperty(value = "成型排程计划合计")
    private Double productNum;

    /**
     * 前日二班修正量
     */
    @ImportValidated(digits = true,min = 0,max = 99999999)
    @Excel(name = "ui.data.column.cxScheduleResult.class3PlanedQty")
    @ApiModelProperty(value = "成型排程当日早班计划量，用于半部件计算预计库存")
    private Integer class3PlannedQty;

    /**
     * 一班计划顺序
     */
//    @ImportValidated(digits = true,min = 0,max = 9999999)
//    @Excel(name = "ui.data.column.scheduleResult.class1Sort")
    @ApiModelProperty(value = "一班计划顺序")
    private Integer class1Sort;

    /**
     * 一班计划数
     */
//    @ImportValidated(digits = true,min = 0,max = 9999999)
//    @Excel(name = "ui.data.column.scheduleResult.class1PlanQty")
    @ApiModelProperty(value = "一班计划数")
    private Integer class1PlanQty;

    /**
     * 一班胎胚生产开始时间
     */
    @ApiModelProperty(value = "一班胎胚生产开始时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class1StartTime;

    /**
     * 一班胎胚生产结束时间
     */
    @ApiModelProperty(value = "一班胎胚生产结束时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class1EndTime;

    /**
     * 一班原因分析手工输入
     */
//    @ImportValidated(maxLength = 66)
//    @Excel(name = "ui.data.column.scheduleResult.class1AnalysisInput")
    @ApiModelProperty(value = "一班原因分析手工输入")
    private String class1AnalysisInput;

    /**
     * 一班原因分析
     */
    @ApiModelProperty(value = "一班原因分析")
    private String class1Analysis;

    /**
     * 一班完成量
     */
    @ApiModelProperty(value = "一班完成量")
    private Integer class1FinishQty;

    /**
     * 一班完成率
     */
    @ApiModelProperty(value = "一班完成率")
    private Integer class1FinishRate;

    /**
     * 二班计划顺序
     */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class2Sort")
    @ApiModelProperty(value = "二班计划顺序")
    private Integer class2Sort;

    /**
     * 二班计划数
     */
    @ImportValidated(digits = true,min=0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class2PlanQty")
    @ApiModelProperty(value = "二班计划数")
    private Integer class2PlanQty;

    /**
     * 二班胎胚生产开始时间
     */
    @ApiModelProperty(value = "二班胎胚生产开始时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class2StartTime;

    /**
     * 二班胎胚生产结束时间
     */
    @ApiModelProperty(value = "二班胎胚生产结束时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class2EndTime;

    /**
     * 二班原因分析手工输入
     */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class2AnalysisInput")
    @ApiModelProperty(value = "二班原因分析手工输入")
    private String class2AnalysisInput;

    /**
     * 二班原因分析
     */
    @ApiModelProperty(value = "二班原因分析")
    private String class2Analysis;

    /**
     * 二班完成量
     */
    @ApiModelProperty(value = "二班完成量")
    private Integer class2FinishQty;

    /**
     * 二班完成率
     */
    @ApiModelProperty(value = "二班完成率")
    private Integer class2FinishRate;

    /**
     * 三班计划顺序
     */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class3Sort")
    @ApiModelProperty(value = "三班计划顺序")
    private Integer class3Sort;

    /**
     * 三班计划数
     */
    @ImportValidated(digits = true,min =0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class3PlanQty")
    @ApiModelProperty(value = "三班计划数")
    private Integer class3PlanQty;

    /**
     * 三班胎胚生产开始时间
     */
    @ApiModelProperty(value = "三班胎胚生产开始时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class3StartTime;

    /**
     * 三班胎胚生产结束时间
     */
    @ApiModelProperty(value = "三班胎胚生产结束时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class3EndTime;

    /**
     * 三班原因分析
     */
    @ApiModelProperty(value = "三班原因分析")
    private String class3Analysis;

    /**
     * 三班原因分析手工输入
     */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class3AnalysisInput")
    @ApiModelProperty(value = "三班原因分析手工输入")
    private String class3AnalysisInput;

    /**
     * 三班完成量
     */
    @ApiModelProperty(value = "三班完成量")
    private Integer class3FinishQty;

    /**
     * 三班完成率
     */
    @ApiModelProperty(value = "三班完成率")
    private Integer class3FinishRate;

    /**
     * 次日一班顺序
     */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class4Sort")
    @ApiModelProperty(value = "次日一班顺序")
    private Integer class4Sort;

    /**
     * 次日一班计划数
     */
    @ImportValidated(digits = true,min=0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class4PlanQty")
    @ApiModelProperty(value = "次日一班计划数")
    private Integer class4PlanQty;

    /**
     * 次日一班胎胚生产开始时间
     */
    @ApiModelProperty(value = "次日一班胎胚生产开始时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class4StartTime;

    /**
     * 次日一班胎胚生产结束时间
     */
    @ApiModelProperty(value = "次日一班胎胚生产结束时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class4EndTime;

    /**
     * 次日一班原因分析
     */
    @ApiModelProperty(value = "次日一班原因分析")
    private String class4Analysis;

    /**
     * 次日一班原因分析手工输入
     */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class4AnalysisInput")
    @ApiModelProperty(value = "次日一班原因分析手工输入")
    private String class4AnalysisInput;

    /**
     * 次日一班完成量
     */
    @ApiModelProperty(value = "次日一班完成量")
    private Integer class4FinishQty;

    /**
     * 次日一班完成率
     */
    @ApiModelProperty(value = "次日一班完成率")
    private Integer class4FinishRate;

    /**
     * 次日二班顺序
     */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class5Sort")
    @ApiModelProperty(value = "次日二班顺序")
    private Integer class5Sort;

    /**
     * 次日二班计划数
     */
    @ImportValidated(digits = true,min=0,max = 9999999)
    @Excel(name = "ui.data.column.result.class5PlanQty")
    @ApiModelProperty(value = "次日二班计划数")
    private Integer class5PlanQty;

    /**
     * 次日二班胎胚生产开始时间
     */
    @ApiModelProperty(value = "次日二班胎胚生产开始时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class5StartTime;

    /**
     * 次日二班胎胚生产结束时间
     */
    @ApiModelProperty(value = "次日二班胎胚生产结束时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class5EndTime;


    /**
     * 次日二班原因分析
     */
    @ApiModelProperty(value = "次日二班原因分析")
    private String class5Analysis;

    /**
     * 次日二班原因分析手工输入
     */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class5AnalysisInput")
    @ApiModelProperty(value = "次日二班原因分析手工输入")
    private String class5AnalysisInput;


    /**
     * 次日二班完成量
     */
    @ApiModelProperty(value = "次日二班完成量")
    private Integer class5FinishQty;

    /**
     * 次日二班完成率
     */
    @ApiModelProperty(value = "次日二班完成率")
    private Integer class5FinishRate;

    /**
     * 次日三班顺序
     */
    @ApiModelProperty(value = "次日三班顺序")
    private Integer class6Sort;

    /**
     * 次日三班计划数
     */
    @ApiModelProperty(value = "次日三班计划数")
    private Integer class6PlanQty;

    /**
     * 次日三班胎胚生产开始时间
     */
    @ApiModelProperty(value = "次日三班胎胚生产开始时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class6StartTime;

    /**
     * 次日三班胎胚生产结束时间
     */
    @ApiModelProperty(value = "次日三班胎胚生产结束时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date class6EndTime;

    /**
     * 次日三班原因分析
     */
    @ApiModelProperty(value = "次日三班原因分析")
    private String class6Analysis;

    /**
     * 次日三班原因分析手工输入
     */
    @ApiModelProperty(value = "次日三班原因分析手工输入")
    private String class6AnalysisInput;

    /**
     * 次日三班完成量
     */
    @ApiModelProperty(value = "次日三班完成量")
    private Integer class6FinishQty;

    /**
     * 次日三班完成率
     */
    @ApiModelProperty(value = "次日三班完成率")
    private Integer class6FinishRate;

    /**
     * 收尾提示标识
     */
    @ApiModelProperty(value = "收尾提示标识")
    private String markCloseOutTip;


    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    private String remark;

    /**
     * 欠胎时间
     */
    @ApiModelProperty(value = "欠胎时间")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd hh:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss",timezone = "GMT+8")
    private Date previousTireTime;

    /**
     * 排程日期查询字符串条件
     */
    @TableField(exist = false)
    private String scheduleDateStr;
    @TableField(exist = false)
    private String startTime;
    @TableField(exist = false)
    private String endTime;

    @ApiModelProperty(value = "颜色类型")
    @TableField(exist = false)
    private String colorType;

    @ApiModelProperty(value = "颜色代码")
    @TableField(exist = false)
    private String colorCode;


    /**
     *  joran 2021-10-04 硫化引擎查询成型排程限定条件属性
     */
    @TableField(exist = false)
    private Integer  limit;

    /**
     * Joran 2021-10-04 化引擎查询成型排程根据日期进行排序
     */
    @TableField(exist = false)
    private String sortByScheduleDate;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 特殊要求
     */
    private String specialRequirements;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    /**
     * 半部件删除标识
     */
    @ApiModelProperty(value = "半部件删除标识")
    private Integer delFlag;

    /**
     * 操作标识符
     */
    @TableField(exist = false)
    private String local;


    @ApiModelProperty(value = "排程记录id数组")
    @TableField(exist = false)
    private Long[] ids;

    /**
     * 班制
     */
    @ApiModelProperty(value = "班制")
    private Integer workShifts;

    /**
     * 库存地点
     */
    @Excel(name = "ui.data.column.cxScheduleResult.storageLocation",dictType = "STORAGE_LOCATION")
    @ApiModelProperty(value = "库存地点")
    private String storageLocation;

    @ApiModelProperty(value = "是否存在版本")
    @TableField(exist = false)
    private Integer hasVersion;

    /**
     * 标记是否投产 0：是；1：否，处理同胎胚只投产一个
     */
    @TableField(exist = false)
    private String toProduct;
}
