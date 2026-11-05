package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 新行提前/停放与静态口型校验输入，不读取数据库、不保存生产事实。
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
     * 试排节点生产开始/完成时点，epoch毫秒。
     */
    private Long productionStartTime;
    private Long productionEndTime;
    /**
     * 供料前最小停放小时，来自原SYS0805001/SYS0905001，不是保质期。
     */
    private BigDecimal minimumStandingHours;
    /**
     * 工厂最大停放小时，新参数默认72，0仅表示关闭最大时效限制。
     */
    private BigDecimal maximumStandingHours;
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
