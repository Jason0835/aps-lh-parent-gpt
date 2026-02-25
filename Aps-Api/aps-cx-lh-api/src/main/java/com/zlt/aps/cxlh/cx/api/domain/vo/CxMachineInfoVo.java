package com.zlt.aps.cxlh.cx.api.domain.vo;


import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachineCls;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachineClsB;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachineStatus;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 成型排程使用成型机档案Vo
 * @author 16799
 */
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "基础数据-成型机档案对象", description = "基础数据-成型机档案对象 ")
@Data
public class CxMachineInfoVo extends MdmMoldingMachine {
    private static final long serialVersionUID = 1L;

    /**
     * 成型机类型
     */
    private MdmMoldingMachineCls moldingMachineCls;

    /**
     * 成型机类型子表
     */
    private List<MdmMoldingMachineClsB> moldingMachineClassList;

    /**
     * 成型机状态
     */
    private MdmMoldingMachineStatus mdmMoldingMachineStatus;

    /**
     * 成型机可用开始时间
     */
    private LocalDateTime availableBeginTime;

    /**
     * 成型机可用结束时间
     */
    private LocalDateTime availableEndTime;

    /**
     * 机台当天剩余时间
     */
    private BigDecimal remainTime;

    /**
     * 成型机台依据安排任务数
     */
    private Integer taskNum= 0;

    /** 机台当前安排规格是否交期，0--否，1--是 */
    private String isDelivery = "0";

    /**
     * 机台当前安排的规格
     */
    private LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto = null;

    /**
     * 成型机台换工装次数
     */
    private Integer class1ChangeNum = 0;
    private Integer class2ChangeNum = 0;
    private Integer class3ChangeNum = 0;
    private Integer class4ChangeNum = 0;
    private Integer class5ChangeNum = 0;
    private Integer class6ChangeNum = 0;

    /**
     * 机台最后安排的任务对应的施工信息
     */
    private CxProductConstructionInfoDto cxProductConstructionInfo = null;

    /**
     * 机台预排任务的施工相似度
     */
    private Integer similarity = 0;


    /** 机台预排任务的剩余产能*/
    private Integer remainCapacity = 0;


    /** 机台是否预排任务的历史机台*/
    private Boolean isHistoryMachine = false;


    /**
     * 机台当前班次
     */
    private int currentShift = 0;

    /**
     * 机台班次顺序
     */
    private int currentShiftSort;

    /**
     * 机台是否落点有效
     */
    private Boolean isPoint = false;
}