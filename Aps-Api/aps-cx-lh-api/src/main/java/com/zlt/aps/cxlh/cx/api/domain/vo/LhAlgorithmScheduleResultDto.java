package com.zlt.aps.cxlh.cx.api.domain.vo;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.monthplan.api.domain.entity.CxEmbryoMonthPlanSurplus;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved.
 * 文件名称：LhAlgorithmScheduleResultVo.java
 * 描述：成型排程使用
 * @author nick +
 */
@ApiModel(value = "成型算法硫化排程结果对象", description = "成型算法硫化排程结果对象 ")
@Data
public class LhAlgorithmScheduleResultDto extends BaseEntity implements Serializable {

    /**
     * 硫化排程结果
     */
    @ApiModelProperty(value = "硫化排程结果", name = "lhScheduleResult")
    public LhScheduleResult lhScheduleResult;

    /**
     * 胎胚欠胎时间
     */
    @ApiModelProperty(value = "胎胚欠胎时间", name = "previousTireTime")
    private LocalDateTime previousTireTime;

    /**
     * 期初库存
     */
    @ApiModelProperty(value = "期初库存", name = "initialInventory")
    private Integer initialInventory = 0;

    /**
     * 批次号
     */
    @ApiModelProperty(value = "批次号", name = "batchNo")
    private String batchNo;


    //--------------------------------------标记字段Start--------------------------------
    /**
     * 是否续作胎胚
     */
    @ApiModelProperty(value = "是否续作胎胚", name = "isContinueTire")
    private Boolean isContinueTire = false;

    /**
     * 是否新增胎胚
     */
    @ApiModelProperty(value = "是否新增胎胚", name = "isNewTire")
    private Boolean isNewTire = false;

    /**
     * 库存满足规格
     */
    @ApiModelProperty(value = "库存满足规格", name = "isSatisfySpecification")
    private Boolean isSatisfySpecification = false;

    /**
     * 是否限制
     */
    @ApiModelProperty(value = "是否限制", name = "isLimitTire")
    private Boolean isLimitTire = false;

    /**
     * 是否收尾胎胚
     */
    @ApiModelProperty(value = "是否收尾胎胚", name = "isEndTire")
    private Boolean isEndTire = false;

    /**
     * 是否普通胎胚
     */
    @ApiModelProperty(value = "是否普通胎胚", name = "isNormalTire")
    private Boolean isNormalTire = true;

    /**
     * 是否大规格胎胚
     */
    @ApiModelProperty(value = "是否大规格胎胚", name = "isLargeTire")
    private Boolean isLargeTire = false;

    /**
     * 是否小规格胎胚
     */
    @ApiModelProperty(value = "是否小规格胎胚", name = "isSmallTire")
    private Boolean isSmallTire = true;

    /**
     * 历史占用机台
     */
    @ApiModelProperty(value = "历史占用机台", name = "lastOccupiedMachines")
    private HashSet<String> lastOccupiedMachines = new HashSet<>();

    /**
     * 限制生产的机台
     */
    @ApiModelProperty(value = "限制生产的机台", name = "limitMachines")
    private HashSet<String>  limitMachines = new HashSet<>();

    /**
     * 禁止生产的机台
     */
    @ApiModelProperty(value = "禁止生产的机台", name = "forbidMachines")
    private HashSet<String>  forbidMachines = new HashSet<>();

    /**
     * 可选机台列表
     */
    @ApiModelProperty(value = "可选机台列表", name = "optionalMachines")
    private HashSet<String>  optionalMachines = new HashSet<>();

    /**
     * 是否安排结束
     */
    @ApiModelProperty(value = "是否安排结束", name = "isScheduleEnd")
    private Boolean isScheduleEnd = Boolean.FALSE;

    /**
     * 是否停排
     */
    @ApiModelProperty(value = "是否停排", name = "isStopSchedule")
    private Boolean isStopSchedule = Boolean.FALSE;

    /**
     * 停排描述
     */
    @ApiModelProperty(value = "停排描述", name = "stopScheduleDesc")
    private String stopScheduleDesc;

    /**
     * 成型胎胚月度汇总表
     */
    @ApiModelProperty(value = "成型胎胚月度汇总表", name = "cxEmbryoMonthPlanSurplus")
    private CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus;

    /**
     *  成型胎胚施工信息表
     */
    @ApiModelProperty(value = "成型胎胚施工信息表", name = "cxProductConstructionInfoDto")
    private CxProductConstructionInfoDto cxProductConstructionInfoDto;

    /**
     * 规格物料信息列表
     */
    @ApiModelProperty(value = "规格物料信息列表", name = "mdmProductInfoList")
    private List<MdmProductInfo> mdmProductInfoList;

    /**
     * 胎胚拆单计数器
     */
    @ApiModelProperty(value = "胎胚拆单计数器", name = "tireCount")
    private Integer tireCount = 0;

    /**
     * 任务计划量
     */
    @ApiModelProperty(value = "任务计划量", name = "taskPlanQuantity")
    private Integer taskPlanQuantity = 0;


    /**
     * 计划量
     */
    @ApiModelProperty(value = "1班计划量", name = "taskPlanQuantity")
    private Integer class1PlanQty;
    @ApiModelProperty(value = "2班计划量", name = "taskPlanQuantity")
    private Integer class2PlanQty;
    @ApiModelProperty(value = "3班计划量", name = "taskPlanQuantity")
    private Integer class3PlanQty;

    /**
     * 计划顺序
     */
    private Integer class1Sort;
    private Integer class2Sort;
    private Integer class3Sort;

    /**
     * 机台定额
     */
    private Integer class1MachineQty;
    private Integer class2MachineQty;
    private Integer class3MachineQty;

    /**
     * 计划开始时间
     */
    private LocalDateTime class1StartTime;
    private LocalDateTime class2StartTime;
    private LocalDateTime class3StartTime;

    /**
     * 计划结束时间
     */
    private LocalDateTime class1EndTime;
    private LocalDateTime class2EndTime;
    private LocalDateTime class3EndTime;

    /**
     * 原因分析
     */
    private String class1Analysis;
    private String class2Analysis;
    private String class3Analysis;


    /**
     * 机台默认定额
     */
    private Integer classMachineDefaultQty;

    /**
     * 收尾描述
     */
    private String endTireDesc;

    /**
     * 胎胚开始生产的时间
     */
    private LocalDateTime startTime;

    /**
     * 胎胚结束生产的时间
     */
    private LocalDateTime endTime;

    /**
     * 任务最终指定安排的机台
     */
    private String finalMachine;

    /**
     * 可硫化班数
     */
    private double classShiftNum;

    /**
     * 无参构造函数
     */
    public LhAlgorithmScheduleResultDto() {
    }

    /**
     * 停排原因
     */
    private String stopScheduleReason;

    /**
     * 补偿量
     */
    private Long compensationQty;

    /**
     * 最大备胎量
     */
    private Double maxTireQty;


    /**
     * 最大压缩量
     */
    private Long maxCompressionQty;


    /**
     * 复制对象专用的构造函数
     */
    public LhAlgorithmScheduleResultDto(LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto) {
        // 对象中的实体采用copyProperties复制
        this.lhScheduleResult = new LhScheduleResult();
        BeanUtils.copyProperties(lhAlgorithmScheduleResultDto.getLhScheduleResult(),this.lhScheduleResult);
        this.previousTireTime = lhAlgorithmScheduleResultDto.previousTireTime;
        this.isContinueTire = lhAlgorithmScheduleResultDto.isContinueTire;
        this.isNewTire = lhAlgorithmScheduleResultDto.isNewTire;
        this.isLimitTire = lhAlgorithmScheduleResultDto.isLimitTire;
        this.isEndTire = lhAlgorithmScheduleResultDto.isEndTire;
        this.isNormalTire = lhAlgorithmScheduleResultDto.isNormalTire;
        this.isLargeTire = lhAlgorithmScheduleResultDto.isLargeTire;
        this.isSmallTire = lhAlgorithmScheduleResultDto.isSmallTire;
        this.lastOccupiedMachines = lhAlgorithmScheduleResultDto.lastOccupiedMachines;
        this.limitMachines = lhAlgorithmScheduleResultDto.limitMachines;
        this.forbidMachines = lhAlgorithmScheduleResultDto.forbidMachines;
        this.optionalMachines = lhAlgorithmScheduleResultDto.optionalMachines;
        this.isScheduleEnd = lhAlgorithmScheduleResultDto.isScheduleEnd;
        this.isStopSchedule = lhAlgorithmScheduleResultDto.isStopSchedule;

        if (lhAlgorithmScheduleResultDto.getCxEmbryoMonthPlanSurplus() != null){
            this.cxEmbryoMonthPlanSurplus = new CxEmbryoMonthPlanSurplus();
            BeanUtils.copyProperties(lhAlgorithmScheduleResultDto.getCxEmbryoMonthPlanSurplus(), this.cxEmbryoMonthPlanSurplus);
        }

        if (lhAlgorithmScheduleResultDto.getCxProductConstructionInfoDto() != null){
            this.cxProductConstructionInfoDto = new CxProductConstructionInfoDto();
            BeanUtils.copyProperties(lhAlgorithmScheduleResultDto.getCxProductConstructionInfoDto(), this.cxProductConstructionInfoDto);
        }

        this.mdmProductInfoList = new ArrayList<>( lhAlgorithmScheduleResultDto.mdmProductInfoList);
        this.tireCount = lhAlgorithmScheduleResultDto.getTireCount();
        this.taskPlanQuantity = lhAlgorithmScheduleResultDto.getTaskPlanQuantity();
        this.class1PlanQty = lhAlgorithmScheduleResultDto.getClass1PlanQty();
        this.class2PlanQty = lhAlgorithmScheduleResultDto.getClass2PlanQty();
        this.class3PlanQty = lhAlgorithmScheduleResultDto.getClass3PlanQty();
        this.class1Sort = lhAlgorithmScheduleResultDto.getClass1Sort();
        this.class2Sort = lhAlgorithmScheduleResultDto.getClass2Sort();
        this.class3Sort = lhAlgorithmScheduleResultDto.getClass3Sort();
        this.class1MachineQty = lhAlgorithmScheduleResultDto.getClass1MachineQty();
        this.class2MachineQty = lhAlgorithmScheduleResultDto.getClass2MachineQty();
        this.class3MachineQty = lhAlgorithmScheduleResultDto.getClass3MachineQty();
        this.classMachineDefaultQty = lhAlgorithmScheduleResultDto.getClassMachineDefaultQty();
        this.startTime = lhAlgorithmScheduleResultDto.getStartTime();
        this.finalMachine = lhAlgorithmScheduleResultDto.getFinalMachine();
        this.classShiftNum = lhAlgorithmScheduleResultDto.getClassShiftNum();
    }
}
