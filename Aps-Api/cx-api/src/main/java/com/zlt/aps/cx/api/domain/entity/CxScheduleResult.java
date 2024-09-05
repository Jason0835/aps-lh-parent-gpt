package com.zlt.aps.cx.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 成型排程结果对象 t_cx_schedule_result
 *
 * @author zlt
 * @date 2021-07-12
 */
@Data
@ApiModel(value = "成型排程结果对象", description = "成型排程结果对象 ")
public class CxScheduleResult extends ApsBaseEntity implements Cloneable {

    private static final long serialVersionUID = 1L;

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
     * 任务类型
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.taskType")
    @ApiModelProperty(value = "任务类型")
    private String taskType;

    /**
     * 排程日期
     */
    @ImportValidated(required = true, date = true)
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /**
     * 生产状态
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.productionStatus")
    @ApiModelProperty(value = "生产状态")
    private String productionStatus;

    /**
     * 成型机台编号
     */
    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;


    @ApiModelProperty(value = "成型机台类型：1=一次法；2=二次法")
    private String cxMachineType;

    /**
     * 成型机台名称
     */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.cxScheduleResult.cxMachineCode")
    @ApiModelProperty(value = "成型机台名称")
    private String cxMachineName;

    /**
     * 硫化机台编号
     */
//    @Excel(name = "ui.data.column.cxScheduleResult.lhMachineCode1")
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    /**
     * 硫化机台名称
     */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.cxScheduleResult.lhMachineCode")
    @ApiModelProperty(value = "硫化机台名称")
    private String lhMachineName;

    /**
     * 使用模数
     */
    @ImportValidated(number = true,min=0,max = 99)
    @Excel(name = "ui.data.column.cxScheduleResult.lhMachineQty")
    @ApiModelProperty(value = "使用模数")
    private Double lhMachineQty;

    /**
     * 最小硫化机需求数
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.minimumLhMachineReqQty")
    @ApiModelProperty(value = "最小硫化机需求数")
    private Double minimumLhMachineReqQty;

    @ApiModelProperty(value = "最小硫化机数")
    private Double minimumLhMachineComQty;

    /**
     * 可用模具数量
     */
    @ImportValidated(digits = true,min=0,max = 99)
    @Excel(name = "ui.data.column.cxScheduleResult.availableMoldQty")
    @ApiModelProperty(value = "可用模数")
    private Integer availableMoldQty;

    /**
     * 最大班数
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.maximumClassQty")
    @ApiModelProperty(value = "最大班数")
    private Double maximumClassQty;

    /**
     * 班制
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.workShifts")
    @ApiModelProperty(value = "班制")
    private Integer workShifts;

    /**
     * 库存地点
     */
    @ImportValidated(required = true,maxLength = 10)
    @Excel(name = "ui.data.column.cxScheduleResult.storageLocation",dictType = "STORAGE_LOCATION")
    @ApiModelProperty(value = "库存地点")
    private String storageLocation;

    /**
     * SAP品号
     */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.cxScheduleResult.sapCode")
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    /**
     * 规格型号
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.cxScheduleResult.specDesc")
    @ApiModelProperty(value = "规格型号")
    private String specDesc;

    /**
     * 胎胚代码
     */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.cxScheduleResult.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /**
     * 施工版本信息
     */
    @Excel(name = "ui.data.column.productStatus.bomDataVersion")
    private  String bomDataVersion;

    /**
     * 硫化中夜班产量（昨天16点到今天8点）
     */
    @ImportValidated(digits = true,min = 0,max = 99999999)
    @Excel(name = "ui.data.column.cxScheduleResult.lhMiddleNightFinishQty")
    @ApiModelProperty(value = "硫化中夜班产量")
    private Integer lhMiddleNightFinishQty;

    /**
     * 成型产量（本月成型总量）
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.cxMonthFinishQty", readConverterExp = "本=月成型总量")
    @ApiModelProperty(value = "成型产量")
    private Integer cxMonthFinishQty;

    /**
     * 废次品数量
     */
    @ImportValidated(digits = true,min = 0,max = 99999999)
    @Excel(name = "ui.data.column.cxScheduleResult.rejectQty")
    @ApiModelProperty(value = "废次品数量")
    private Integer rejectQty;

    /**
     * 最新计划数(初稿)
     */
    @ImportValidated(digits = true,min = 0,max = 99999999)
    @Excel(name = "ui.data.column.cxScheduleResult.newestPlanQty")
    @ApiModelProperty(value = "最新计划数(初稿)")
    private Integer newestPlanQty;

    /**
     * 三班(8点-16点)计划量
     */
    @ImportValidated(digits = true,min = 0,max = 99999999)
    @Excel(name = "ui.data.column.cxScheduleResult.class3PlannedQty")
    @ApiModelProperty(value = "三班(8点-16点)计划量")
    private Integer class3PlannedQty;

    /**
     * 单班硫化量
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.singleShiftLhQty")
    @ApiModelProperty(value = "单班硫化量")
    private Integer singleShiftLhQty;

    /**
     * 总库存数量
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.totalStock")
    @ApiModelProperty(value = "总库存数量")
    private Integer totalStock;

    /**
     * 超期库存数量
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.extendedStock")
    @ApiModelProperty(value = "超期库存数量")
    private Integer extendedStock;

    /**
     * 月结库存数量
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.monthStock")
    @ApiModelProperty(value = "月结库存数量")
    private Integer monthStock;

    /**
     * 实际超欠产
     */
    @ImportValidated(digits = true,min = -99999999,max = 99999999)
    @Excel(name = "ui.data.column.cxScheduleResult.actualOverProduction")
    @ApiModelProperty(value = "实际超欠产")
    private Integer actualOverProduction;

    /**
     * 预计超欠产
     */
    @ImportValidated(digits = true,min = 0,max = 99999999)
    @Excel(name = "ui.data.column.cxScheduleResult.expectedOverProduction")
    @ApiModelProperty(value = "预计超欠产")
    private Integer expectedOverProduction;

    /**
     * 超欠产差额(实际-预计)
     */
    @ImportValidated(digits = true,min = -99999999,max = 99999999)
    @Excel(name = "ui.data.column.cxScheduleResult.differenceOverProduction")
    @ApiModelProperty(value = "超欠产差额(实际-预计)")
    private Integer differenceOverProduction;

    /**
     * 一班可硫化班次
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class1AvailableLhShift")
    @ApiModelProperty(value = "一班可硫化班次")
    private Double class1AvailableLhShift;

    /**
     * 一班计划数
     */
    @ImportValidated(digits = true,min = 0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class1PlanQty")
    @ApiModelProperty(value = "一班计划数")
    private Integer class1PlanQty;

    /**
     * 一班原因分析手工输入
     */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class1AnalysisInput")
    @ApiModelProperty(value = "一班原因分析手工输入")
    private String class1AnalysisInput;

    /**
     * 一班原因分析
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class1Analysis")
    @ApiModelProperty(value = "一班原因分析")
    private String class1Analysis;

    /**
     * 一班完成量
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class1FinishQty")
    @ApiModelProperty(value = "一班完成量")
    private Integer class1FinishQty;

    /**
     * 二班可硫化班次
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class2AvailableLhShift")
    @ApiModelProperty(value = "二班可硫化班次")
    private Double class2AvailableLhShift;

    /**
     * 二班计划数
     */
    @ImportValidated(digits = true,min=0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class2PlanQty")
    @ApiModelProperty(value = "二班计划数")
    private Integer class2PlanQty;

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
    //@Excel(name = "ui.data.column.cxScheduleResult.class2Analysis")
    @ApiModelProperty(value = "二班原因分析")
    private String class2Analysis;

    /**
     * 二班完成量
     */
    //@Excel(name = "ui.data.column.scheduleResult.Plan")
    @ApiModelProperty(value = "二班完成量")
    private Integer class2FinishQty;

    /**
     * 三班可硫化班次
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class3AvailableLhShift")
    @ApiModelProperty(value = "三班可硫化班次")
    private Double class3AvailableLhShift;

    /**
     * 三班计划数
     */
    @ImportValidated(digits = true,min =0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class3PlanQty")
    @ApiModelProperty(value = "三班计划数")
    private Integer class3PlanQty;

    /**
     * 三班原因分析
     */
    @ApiModelProperty(value = "三班原因分析")
    private String class3Analysis;

    /**
     * 三班完成量
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class3FinishQty")
    @ApiModelProperty(value = "三班完成量")
    private Integer class3FinishQty;

    /**
     * 次日一班可硫化班次
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class4AvailableLhShift")
    @ApiModelProperty(value = "次日一班可硫化班次")
    private Double class4AvailableLhShift;

    /**
     * 次日一班计划数
     */
    @ImportValidated(digits = true,min=0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class4PlanQty")
    @ApiModelProperty(value = "次日一班计划数")
    private Integer class4PlanQty;

    /**
     * 次日一班原因分析
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class4Analysis")
    @ApiModelProperty(value = "次日一班原因分析")
    private String class4Analysis;

    /**
     * 次日一班完成量
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class4FinishQty")
    @ApiModelProperty(value = "次日一班完成量")
    private Integer class4FinishQty;

    /**
     * 次日二班可硫化班次
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class5AvailableLhShift")
    @ApiModelProperty(value = "次日二班可硫化班次")
    private Double class5AvailableLhShift;

    /**
     * 次日二班计划数
     */
    @ImportValidated(digits = true,min=0,max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.class5PlanQty")
    @ApiModelProperty(value = "次日二班计划数")
    private Integer class5PlanQty;

    /**
     * 次日二班原因分析
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class5Analysis")
    @ApiModelProperty(value = "次日二班原因分析")
    private String class5Analysis;

    /**
     * 次日二班完成量
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class5FinishQty")
    @ApiModelProperty(value = "次日二班完成量")
    private Integer class5FinishQty;

    /**
     * 删除标识
     */
    @ApiModelProperty(value = "次日二班完成量")
    private String delFlag;

    /**
     * 收尾提示标识
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.markCloseOutTip")
    @ApiModelProperty(value = "收尾提示标识")
    private String markCloseOutTip;

    /**
     * 外胎规格尺寸信息
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.specDimension")
    @ApiModelProperty(value = "外胎规格尺寸信息")
    private Double specDimension;

    /**
     * 是否发布
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.isRelease")
    @ApiModelProperty(value = "是否发布")
    private String isRelease;

    /**
     * 三班原因分析手工输入
     */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class3AnalysisInput")
    @ApiModelProperty(value = "三班原因分析手工输入")
    private String class3AnalysisInput;

    /**
     * 次日一班原因分析手工输入
     */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class4AnalysisInput")
    @ApiModelProperty(value = "次日一班原因分析手工输入")
    private String class4AnalysisInput;

    /**
     * 次日二班原因分析手工输入
     */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class5AnalysisInput")
    @ApiModelProperty(value = "次日二班原因分析手工输入")
    private String class5AnalysisInput;


    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    private String remark;

    /**
     * 一班计划顺序
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class1Sort")
    @ApiModelProperty(value = "一班计划顺序")
    private Integer class1Sort;

    /**
     * 二班计划顺序
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class2Sort")
    @ApiModelProperty(value = "二班计划顺序")
    private Integer class2Sort;

    /**
     * 三班计划顺序
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class3Sort")
    @ApiModelProperty(value = "三班计划顺序")
    private Integer class3Sort;

    /**
     * 次日一班计划顺序
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class4Sort")
    @ApiModelProperty(value = "次日一班计划顺序")
    private Integer class4Sort;

    /**
     * 次日二班计划顺序
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.class5Sort")
    @ApiModelProperty(value = "次日二班计划顺序")
    private Integer class5Sort;

    /**
     * 计划修正量
     */
    //@Excel(name = "ui.data.column.cxScheduleResult.planModifyQty")
    @ApiModelProperty(value = "计划修正量")
    private Integer planModifyQty;

    @ApiModelProperty(value = "月计划剩余量")
    private Integer monthPlanOs;

    @ApiModelProperty(value = "月计划量")
    private Integer monthPlan;

    private String local;

    //其他9个半部件调量相关属性
    //15度裁断
    private List<CxScheduleSub> cd15ScheduleList=new ArrayList<>();

    //90度裁断
    private List<CxScheduleSub> cd90ScheduleList=new ArrayList<>();

    //钢带压延
    private List<CxScheduleSub> gdyyScheduleList=new ArrayList<>();

    //钢丝圈
    private List<CxScheduleSub> gsqScheduleList=new ArrayList<>();

    //内衬
    private List<CxScheduleSub> ncScheduleList=new ArrayList<>();

    //胎侧
    private List<CxScheduleSub> tcScheduleList=new ArrayList<>();

    //胎面
    private List<CxScheduleSub> tmScheduleList=new ArrayList<>();

    //胎圈
    private List<CxScheduleSub> tqScheduleList=new ArrayList<>();

    //纤维压延
    private List<CxScheduleSub> xwyyScheduleList=new ArrayList<>();

    @ApiModelProperty(value = "总计划量")
    private Integer totalClassPlanQty;

    /**
     * 更换类型描述：硫化机code1:拆模换code:模数molds;硫化机code2:点数换code:模数molds;
     */
    @ApiModelProperty(value = "确定硫化机以及更换类型")
    private String lhMachineChangeMoldDesc;

    @ApiModelProperty(value = "单胎硫化时长")
    private Double lhSingleTireTime;

    /**
     * 计算模数(引擎使用)
     */
    private Integer calcMoldNum;

    /**
     * 计算模数(引擎使用)
     */
    private Integer calcTotalStock;

    /**
     * 排程日期查询字符串条件
     */
    private String scheduleDateStr;

    //用于多台硫化机及对应的更换类型进行拼接
    private List<LhMachineChangeTpye> lhMachineChangeTpyeList=new ArrayList<>();

    //查询条件开始日期
    private String startTime;

    //查询条件结束日期
    private String endTime;

    @ApiModelProperty(value = "颜色类型")
    private String colorType;

    @ApiModelProperty(value = "颜色代码")
    private String colorCode;

    @ApiModelProperty(value = "总超期库存")
    private Long overTimeStock;

    @ApiModelProperty(value = "月结超期库存")
    private Long overTimeMonthStock;

    /**
     *  joran 2021-10-04 硫化引擎查询成型排程限定条件属性
     */
    private Integer  limit;

    /**
     * Joran 2021-10-04 化引擎查询成型排程根据日期进行排序
     */
    private String sortByScheduleDate;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 特殊要求
     */
    private String specialRequirements;

    //显示颜色属性
    private long colorForLhMachine=0;
    private long colorForSapCode=0;
    private long colorForEmbryoCode=0;

    //排序属性：整体按寸口升序，机台台一样的按寸口升序
    private String orderByStr;

    @ApiModelProperty(value = "是否存在版本")
    private Integer hasVersion;

    @ApiModelProperty(value = "中夜班产量差值")
    private Integer midNightDifferenceQty;

    @ApiModelProperty(value = "调度员是否修改了成型机机，0：否，1：是")
    private Integer changeCxMachine;

    @ApiModelProperty(value = "调度员是否修改了一班计划量，0：否，1：是")
    private Integer changeClass1Plan;

    @ApiModelProperty(value = "调度员是否修改了二班计划量，0：否，1：是")
    private Integer changeClass2Plan;

    @ApiModelProperty(value = "调度员是否修改了三班计划量，0：否，1：是")
    private Integer changeClass3Plan;

    @ApiModelProperty(value = "调度员是否修改了次日一班计划量，0：否，1：是")
    private Integer changeClass4Plan;

    @ApiModelProperty(value = "调度员是否修改了次日二班计划量，0：否，1：是")
    private Integer changeClass5Plan;

    @ApiModelProperty(value = "排程记录id数组")
    private Long[] ids;

    /**
     * 标记是否投产 0：是；1：否，处理同胎胚只投产一个
     */
    private String toProduct;

    @ApiModelProperty(value = "胎胚共用模具信息")
    private String shareMoldInfoStr;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    @ApiModelProperty(value = "当前成型机是否自动停排，0：否，1：是")
    private Integer scheduleStop;

    @ApiModelProperty(value = "当前成型机自动停排班次，0:前一天白班；1：中班；2：夜班；3：白班;4：次一班")
    private Integer stopClassShift;

    @ApiModelProperty(value = "月计划剩余量悬浮显示信息")
    private String monthPlanOsHoverStr;

    /**
     * 机头宽度
     */
    private String noseWidth;

    public Object clone() {
        CxScheduleResult scheduleResult = null;
        try {
            scheduleResult = (CxScheduleResult)super.clone();
        } catch (CloneNotSupportedException e) {
            return new CxScheduleResult();
        }
        return scheduleResult;
    }
}
