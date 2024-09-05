package com.zlt.aps.cx.engine.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
  * 成型排程任务对象
  * @ClassName CxScheduleTask
  * @Description
  * @Author Joran.Zhang
  * @Date 2021/6/22 17:20
  * @Version 1.0
**/
@Data
@ApiModel(value = "成型排程任务对象", description = "成型排程任务对象")
public class CxEngineScheduleResult extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    @ApiModelProperty(value = "自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String cxBatchNo;

    /** 成型排程工单号，自动生成，批次号+4位定长自增序号 */
    @ApiModelProperty(value = "成型排程工单号，自动生成，批次号+4位定长自增序号")
    private String orderNo;

    /** 任务类型：数据字典维护，待投产、待换模、投产中、已收尾、已收尾欠产等 */
    @ApiModelProperty(value = "任务类型：数据字典维护，待投产、待换模、投产中、已收尾、已收尾欠产等")
    private String taskType;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /** 生产状态:0-未生产；1-生产中；2-生产完成 */
    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成")
    private String productionStatus;

    /** 成型机台编号 */
    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    /** 成型机台名称 */
    @ApiModelProperty(value = "成型机台名称")
    private String cxMachineName;

    /**
     * 成型机台类型：一次法；二次法
     */
    private String cxMachineType;


    /** 硫化机台编号 */
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    /** 硫化机台名称 */
    @ApiModelProperty(value = "硫化机台名称")
    private String lhMachineName;

    /** 硫化机台数量 */
    @ApiModelProperty(value = "硫化机台数量")
    private Double lhMachineQty;

    /** 最小硫化机需求数 */
    @ApiModelProperty(value = "最小硫化机需求数")
    private Integer minimumLhMachineReqQty;

    /** 可用模具数量 */
    @ApiModelProperty(value = "可用模具数量")
    private Integer availableMoldQty;

    /** 最大班数 */
    @ApiModelProperty(value = "最大班数")
    private Double maximumClassQty;

    /** 班制 */
    @ApiModelProperty(value = "班制")
    private Integer workShifts;

    /** 库存地点 */
    @ApiModelProperty(value = "库存地点")
    private String storageLocation;

    /** SAP品号 */
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    /** 规格型号 */
    @ApiModelProperty(value = "规格型号")
    private String specDesc;

    @ApiModelProperty(value = "外胎规格尺寸信息")
    private Double specDimension;

    /** 胎胚代码 */
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /** 硫化中夜班产量
     （昨天16点到今天8点） */
    @ApiModelProperty(value = "硫化中夜班产量")
    private Integer lhMiddleNightFinishQty;

    /** 成型产量
     （本月成型总量） */
    @ApiModelProperty(value = "成型产量")
    private Integer cxMonthFinishQty;

    /** 计划修正量 */
    @ApiModelProperty(value = "计划修正量")
    private Integer planModifyQty;

    /** 废次品数量 */
    @ApiModelProperty(value = "废次品数量")
    private Integer rejectQty;

    /** 最新计划数（初稿） */
    @ApiModelProperty(value = "最新计划数")
    private Integer newestPlanQty;

    /** 三班（8点-16点）计划量 */
    @ApiModelProperty(value = "三班")
    private Integer class3PlannedQty;

    /** 单班硫化量 */
    @ApiModelProperty(value = "单班硫化量")
    private Integer singleShiftLhQty;

    /** 总库存数量 */
    @ApiModelProperty(value = "总库存数量")
    private Integer totalStock;

    /** 超期库存数量 */
    @ApiModelProperty(value = "超期库存数量")
    private Integer extendedStock;

    /** 月结库存数量 */
    @ApiModelProperty(value = "月结库存数量")
    private Integer monthStock;

    /** 实际超欠产 */
    @ApiModelProperty(value = "实际超欠产")
    private Integer actualOverProduction;

    /** 预计超欠产 */
    @ApiModelProperty(value = "预计超欠产")
    private Integer expectedOverProduction;

    /** 超欠产差额（实际-预计） */
    @ApiModelProperty(value = "超欠产差额")
    private Integer differenceOverProduction;

    /** 一班可硫化班次 */
    @ApiModelProperty(value = "一班可硫化班次")
    private Double class1AvailableLhShift;

    /** 一班计划数 */
    @ApiModelProperty(value = "一班计划数")
    private Integer class1PlanQty;

    /** 一班原因分析手工输入 */
    @ApiModelProperty(value = "一班原因分析手工输入")
    private String class1AnalysisInput;

    /** 一班原因分析 */
    @ApiModelProperty(value = "一班原因分析")
    private String class1Analysis;

    /** 一班完成量 */
    @ApiModelProperty(value = "一班完成量")
    private Integer class1FinishQty;

    /** 二班可硫化班次 */
    @ApiModelProperty(value = "二班可硫化班次")
    private Double class2AvailableLhShift;

    /** 二班计划数 */
    @ApiModelProperty(value = "二班计划数")
    private Integer class2PlanQty;

    /** 二班原因分析手工输入 */
    @ApiModelProperty(value = "二班原因分析手工输入")
    private String class2AnalysisInput;

    /** 二班原因分析 */
    @ApiModelProperty(value = "二班原因分析")
    private String class2Analysis;

    /** 二班完成量 */
    @ApiModelProperty(value = "二班完成量")
    private Integer class2FinishQty;

    /** 三班可硫化班次 */
    @ApiModelProperty(value = "三班可硫化班次")
    private Double class3AvailableLhShift;

    /** 三班计划数 */
    @ApiModelProperty(value = "三班计划数")
    private Integer class3PlanQty;

    /** 三班原因分析 */
    @ApiModelProperty(value = "三班原因分析")
    private String class3Analysis;

    /** 三班原因分析 */
    @ApiModelProperty(value = "三班原因分析手工输入")
    private String class3AnalysisInput;

    /** 三班完成量 */
    @ApiModelProperty(value = "三班完成量")
    private Integer class3FinishQty;

    /** 次日一班可硫化班次 */
    @ApiModelProperty(value = "次日一班可硫化班次")
    private Double class4AvailableLhShift;

    /** 次日一班计划数 */
    @ApiModelProperty(value = "次日一班计划数")
    private Integer class4PlanQty;

    /** 次日一班原因分析 */
    @ApiModelProperty(value = "次日一班原因分析")
    private String class4Analysis;

    /** 次日一班原因分析 */
    @ApiModelProperty(value = "次日一班原因分析手工输入")
    private String class4AnalysisInput;

    /** 次日一班完成量 */
    @ApiModelProperty(value = "次日一班完成量")
    private Integer class4FinishQty;

    /** 次日二班可硫化班次 */
    @ApiModelProperty(value = "次日二班可硫化班次")
    private Double class5AvailableLhShift;

    /** 次日二班计划数 */
    @ApiModelProperty(value = "次日二班计划数")
    private Integer class5PlanQty;

    /** 次日二班原因分析 */
    @ApiModelProperty(value = "次日二班原因分析")
    private String class5Analysis;

    /** 次日二班原因分析 */
    @ApiModelProperty(value = "次日二班原因分析手工输入")
    private String class5AnalysisInput;


    /** 次日二班完成量 */
    @ApiModelProperty(value = "次日二班完成量")
    private Integer class5FinishQty;

    /**
     * 是否标识收尾提示，0-是；1-否
     */
    @ApiModelProperty(value = "标识收尾提示")
    private String markCloseOutTip;

    /**
     * 冗余月度剩余量，后续计划安排需要用到
     */
    private Integer monthRemainQty;

    /**
     * 成型月度完成量
     */
    private Integer monthFinishQty;

    /**
     * 冗余前一天的工单号，用于查找剩余任务量
     */
    private String lastOrderNo;

    /**
     *  前一天一班计划量
     */
    private Integer lastClass1PlanQty;

    /**
     * 前一天二班计划量
     */
    private Integer lastClass2PlanQty;

    /**
     * 前一天三班计划量
     */
    private Integer lastClass3PlanQty;

    /**
     * 前一天次日一班计划量
     */
    private Integer lastClass4PlanQty;

    /**
     * 前一天次日二班计划量
     */
    private Integer lastClass5PlanQty;

    /**
     *  单班时长
     */
    private Double classShiftHour;

    /**
     * 任务安排后，班次剩余时长
     */
    private Double remainTime;

    /**
     * 其他规格需要开始的班次
     */
    private Integer nextClassIndex;

    /**
     * 前规格胎胚代码
     */
    private String  lastEmbryoCode;

    /**
     * 一班任务顺序
     */
    private Integer class1Sort;

    /**
     * 二班任务顺序
     */
    private Integer class2Sort;

    /**
     * 三班任务顺序
     */
    private Integer class3Sort;

    /**
     * 四班（次日一班）任务顺序
     */
    private Integer class4Sort;

    /**
     * 五班（次日二班）任务顺序
     */
    private Integer class5Sort;

    /**
     * 数据来源：0》自动排程；1：插单
     */
    private String  dataSource;

    /**
     * 是否为添加新规格，原因分析时使用
     */
    private Boolean newSpecFlag=false;

    /**
     * 发布状态
     */
    public String isRelease;

    /**
     * 2021-07-23 Joran.zhang
     * 按胎胚所在排程硫化机模数占比计算库存，后续可硫化班次按这个库存来计算
     */
    public Integer calcTotalStock;

    /**
     * 计算出来的模数
     */
    public Integer calcMoldNum;

    /**
     * 2021-08-04 单胎硫化时长（分钟，从施工信息表中获取）
     */
    private Double lhSingleTireTime;

    /**
     * 成型排程日期字符串
     */
    private String cxScheduleDate;

    /**
     * 新增字段，确定硫化机 添加多台硫化机及对应的更换类型进行拼接
     * 如 硫化机1:拆模换;硫化机2:点数换;
     */
    @ApiModelProperty(value = "确定硫化机以及更换类型")
    private String lhMachineChangeMoldDesc;

    /**
     * 生产排程对应的生产排程版本号，属性只是用于关联查询存储
     */
    private  String apsMonthVersion;

    /**
     * 施工版本
     */
    private String bomDataVersion;

    /**
     * 特殊要求
     */
    private String specialRequirements;

    /**
     * 标记是否投产 0：是；1：否，处理同胎胚只投产一个
     */
    private String toProduct;

    /** 留存合并前单班硫化量 */
    @ApiModelProperty(value = "留存合并前单班硫化量")
    private Integer beforeSingleShiftLhQty;

    /**
     * Joran 2021-12-22 标记月度汇总表中已经收尾 挑选到这种规格时不允许在进行挑选，且投产表状态要变更为已投产状态
     */
    private Boolean isProducted;

    /** 最小硫化机比对数 */
    @ApiModelProperty(value = "最小硫化机比对数")
    private Double minimumLhMachineComQty;

    /**
     *  第一个标记新投产规格是换工装开班，后续只要换工装即可
     */
    private Boolean markNewSpecAnalysisFlag=false;

    /**
     * 自动排程时同一个记录的班次要第一次算完后要锁定后续直接返回即可
     */
    private Boolean markMaxLhShiftLock=false;

    /**
     * Joran 2022-02-09 添加一个属性用于前一天三班增补时同机台任务进行标记顺序
     */
    private Integer planSort;

    /** 成型增补计划批次号 */
    @ApiModelProperty(value = "成型增补计划批次号")
    private String suppleBatchNo;

    /** 计划增补量 */
    @Excel(name = "ui.data.column.plan.supplePlanQty")
    @ApiModelProperty(value = "计划增补量")
    private Integer supplePlanQty;

    /**
     * 白班可硫化班次时间
     */
    private Double class3PlannedAvailableLhShift;

    /**
     * 增补总量，根据硫化机和班次连续来确定增补量
     */
    private Integer suppleTotalQty;

    @Override
    public String toString() {
        return "排程结果表{" +
                "id=" + id +
                ", 成型批次号='" + cxBatchNo + '\'' +
                ", 排程日期=" + DateUtils.parseDateToStr("yyyyMMdd",scheduleDate) +
                ", 机台编号='" + cxMachineCode + '\'' +
                ", 库存地点='" + storageLocation + '\'' +
                ", SAP品号='" + sapCode + '\'' +
                ", 寸口=" + specDimension +
                ", 胎胚代码='" + embryoCode + '\'' +
                ", 单班硫化量=" + singleShiftLhQty +
                ", 库存=" + totalStock +
                ", 1班可硫化班次=" + class1AvailableLhShift +
                ", 1班计划量=" + class1PlanQty +
                ", 1班原因分析='" + class1Analysis + '\'' +
                ", 2班可硫化班次=" + class2AvailableLhShift +
                ", 2班计划量=" + class2PlanQty +
                ", 2班原因分析='" + class2Analysis + '\'' +
                ", 3班可硫化班次=" + class3AvailableLhShift +
                ", 3班计划量=" + class3PlanQty +
                ", 3班原因分析='" + class3Analysis + '\'' +
                ", 4班可硫化班次=" + class4AvailableLhShift +
                ", 4班计划量=" + class4PlanQty +
                ", 4班原因分析='" + class4Analysis + '\'' +
                ", 5班可硫化班次=" + class5AvailableLhShift +
                ", 5班计划量=" + class5PlanQty +
                ", 5班原因分析='" + class5Analysis + '\'' +
                ", 收尾标识='" + markCloseOutTip + '\'' +
                ", 月剩余量=" + monthRemainQty +
                ", 前一天1班计划量=" + lastClass1PlanQty +
                ", 前一天2班计划量=" + lastClass2PlanQty +
                ", 前一天3班计划量=" + lastClass3PlanQty +
                ", 前一天4班计划量=" + lastClass4PlanQty +
                ", 前一天5班计划量=" + lastClass5PlanQty +
                ", 剩余时间=" + remainTime +
                ", 1班计划排序=" + class1Sort +
                ", 2班计划排序=" + class2Sort +
                ", 3班计划排序=" + class3Sort +
                ", 4班计划排序=" + class4Sort +
                ", 5班计划排序=" + class5Sort +
                '}';
    }



    /**
     * 根据工单号判断是否相同
     * @param obj
     * @return
     */
    public boolean equals (Object obj) {
        CxEngineScheduleResult result = (CxEngineScheduleResult) obj ;
        return this.orderNo.equals(result.orderNo);
    }


    /**
     * 获取扣除当天所有班次计划量后剩余的月度计划量
     * @return
     */
    public Integer calcMonthRemainQty(){
        monthRemainQty=(monthRemainQty==null?0:monthRemainQty);
        lastClass3PlanQty=lastClass3PlanQty==null?0:lastClass3PlanQty;
        if(monthRemainQty>0){
            return this.monthRemainQty-this.lastClass3PlanQty;
        }else{
            return monthRemainQty;
        }

    }

    /**
     * 初始化各个班次计划量
     */
    public void initPlanQty(){
        if(class3PlannedQty==null){
            class3PlannedQty=0;
        }
        if(class1PlanQty==null){
            class1PlanQty=0;
        }
        if(class2PlanQty==null){
            class2PlanQty=0;
        }
        if(class3PlanQty==null){
            class3PlanQty=0;
        }
        if(class4PlanQty==null){
            class4PlanQty=0;
        }
        if(class5PlanQty==null){
            class5PlanQty=0;
        }
    }

    /**
     * 获取5个班次的计划总量
     * @return
     */
    public Integer getDayTotalPlanQty(){
        initPlanQty();
        return  class1PlanQty+class2PlanQty+class3PlanQty+class4PlanQty+class5PlanQty;
    }

    /**
     * 获取三班后的计划总量，汇总进行安排比较快
     * @return
     */
    public Integer getAfterClass3PlanQty(){
        initPlanQty();
        return  class3PlanQty+class4PlanQty+class5PlanQty;
    }


    /**
     * 获取复制前日三班、次一、次二班的计划总量
     * @return
     */
    public Integer getAfterClass3PlannedQty(){
        initPlanQty();
        return  class3PlannedQty+class1PlanQty+class2PlanQty;
    }

    /**
     * 获取前日三班、次一计划总量
     * @return
     */
    public Integer getAfterClass3PlannedTwoShiftQty(){
        initPlanQty();
        return  class3PlannedQty+class1PlanQty;
    }



    public String tailInfo(){
        return "排程结果表{" +
                "id=" + id +
                ", 成型批次号='" + cxBatchNo + '\'' +
                ", 工单号='" + orderNo + '\'' +
                ", 机台编号='" + cxMachineCode + '\'' +
                ", 1班计划量=" + class1PlanQty +
                ", 2班计划量=" + class2PlanQty +
                '}';
    }

}
