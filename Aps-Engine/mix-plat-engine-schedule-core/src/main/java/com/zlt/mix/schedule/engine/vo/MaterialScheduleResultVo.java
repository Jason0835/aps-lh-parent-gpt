package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;

import lombok.Data;

import java.util.Date;

@Data
public class MaterialScheduleResultVo extends MaterialScheduleResult {

//    /**
//     * 计划中班称重完需要的时间（单位：毫秒）
//     */
//    private long midProduceTimeMillis;
//
//    /**
//     * 计划夜班称重完需要的时间（单位：毫秒）
//     */
//    private long nightProduceTimeMillis;
//
//    /**
//     * 计划白班称重完需要的时间（单位：毫秒）
//     */
//    private long dayProduceTimeMillis;

//    /**
//     * 称重机班制(如1--长白班，2--两班制，3--三班制；对应数据字典LH_CLASS_SHIFT)
//     */
//    private Integer classShift;

    /**
     * 单车称重消耗时间（毫秒）
     */
    private Long singleCarTime;

    /**
     * 中班开始时间
     */
    private Date midClassStartTime;

    /**
     * 中班结束时间
     */
    private Date midClassEndTime;

    /**
     * 夜班开始时间
     */
    private Date nightClassStartTime;

    /**
     * 夜班结束时间
     */
    private Date nightClassEndTime;

    /**
     * 白班开始时间
     */
    private Date dayClassStartTime;

    /**
     * 白班结束时间
     */
    private Date dayClassEndTime;
    
    /**
     * 硫化机台
     */
    private LhflMachine machine;
}
