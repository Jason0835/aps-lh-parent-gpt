package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 新行需求窗口与静态口型校验输入，不读取数据库、不保存生产事实。
 *
 * <p>停放上下限与生产起止时点门禁已按业务确认下线，本输入不再承载生产节点时间；
 * 数量上限由目标、产能、工装与成型余量额度在数量计算器中约束。</p>
 */
@Data
public class ScheduleExtraCreateWindowRequest {
    /**
     * 已应用成型偏移后的当前成型逻辑班，禁止再减一次偏移。
     */
    private Integer currentFormingLogicalShiftOrder;
    /**
     * 已按物料匹配深度及批准兜底的有效保证班数。
     */
    private Integer guardShiftCount;
    /**
     * 当前来源口型及同工厂目标机台的静态适配白名单。
     */
    private String mouthPlateCode;
    private Set<String> allowedMouthPlateCodes;
    /**
     * 生产窗口对应的成型日历也必须明确开放；不沿用缺日历默认开放。
     */
    private Boolean productionWindowFormingCalendarOpen;
    /**
     * 同一来源日期轴的成型消费段。
     */
    private List<ScheduleExtraCreateDemandSlice> demandSlices;
}
