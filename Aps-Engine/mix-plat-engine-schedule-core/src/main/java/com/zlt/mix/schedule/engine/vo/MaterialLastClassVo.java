package com.zlt.mix.schedule.engine.vo;

import lombok.Data;

import java.util.Date;

/**
 * 硫磺辅料排程最后一班排在最后的生产顺序，物料名称，预计完成时间
 */
@Data
public class MaterialLastClassVo {

    public MaterialLastClassVo(){

    }

    public MaterialLastClassVo(String materialName, Integer produceOrder, Date expectFinishTime) {
        this.materialName = materialName;
        this.produceOrder = produceOrder;
        this.expectFinishTime = expectFinishTime;
    }
    
    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 白班生产顺序
     */
    private Integer produceOrder;

    /**
     * 计划完成时间
     */
    private Date expectFinishTime;

    /**
     * 所属班制
     */
    private Integer classShift;

    /**
     * 所在班别
     */
    private String classType;
    
    /**
     * 班次开始时间
     */
    private Date classStartTime;
    
    /**
     * 班次结束时间
     */
    private Date classEndTime;
    /**
     * 中班机台是否可用
     */
    private boolean isMachineMidEnable;
    /**
     * 夜班机台是否可用
     */
    private boolean isMachineNightEnable;
    /**
     * 白班机台是否可用
     */
    private boolean isMachineDayEnable;
}
