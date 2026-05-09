package com.zlt.aps.cx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 排程调整结果VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "排程调整结果")
public class ScheduleAdjustResultVo {

    @ApiModelProperty(value = "是否成功")
    private boolean success;

    @ApiModelProperty(value = "消息")
    private String message;

    @ApiModelProperty(value = "调整的班次列表")
    private List<String> adjustedShifts = new ArrayList<>();

    @ApiModelProperty(value = "补车胎胚列表")
    private List<TripAdjustItem> addedTrips = new ArrayList<>();

    @ApiModelProperty(value = "减车胎胚列表")
    private List<TripAdjustItem> removedTrips = new ArrayList<>();

    @ApiModelProperty(value = "胎面供应预警列表")
    private List<TreadSupplyWarning> treadWarnings = new ArrayList<>();

    @ApiModelProperty(value = "顺位重排的记录数")
    private int resequencedCount;

    /**
     * 车次调整项
     */
    @Data
    @ApiModel(value = "车次调整项")
    public static class TripAdjustItem {
        @ApiModelProperty(value = "成型机台编码")
        private String machineCode;

        @ApiModelProperty(value = "胎胚编码")
        private String embryoCode;

        @ApiModelProperty(value = "物料编码")
        private String materialCode;

        @ApiModelProperty(value = "班次")
        private String shiftClass;

        @ApiModelProperty(value = "车次号")
        private Integer tripNo;

        @ApiModelProperty(value = "调整前计划量")
        private Integer beforePlanQty;

        @ApiModelProperty(value = "调整后计划量")
        private Integer afterPlanQty;

        @ApiModelProperty(value = "交班库存时长（小时）")
        private java.math.BigDecimal stockHours;
    }

    /**
     * 胎面供应预警
     */
    @Data
    @ApiModel(value = "胎面供应预警")
    public static class TreadSupplyWarning {
        @ApiModelProperty(value = "成型机台编码")
        private String machineCode;

        @ApiModelProperty(value = "胎胚编码")
        private String embryoCode;

        @ApiModelProperty(value = "物料编码")
        private String materialCode;

        @ApiModelProperty(value = "班次")
        private String shiftClass;

        @ApiModelProperty(value = "车次号")
        private Integer tripNo;

        @ApiModelProperty(value = "成型预计开始时间")
        private String formingStartTime;

        @ApiModelProperty(value = "胎面可供成型时间")
        private String treadAvailableTime;

        @ApiModelProperty(value = "预警类型：SUPPLY_UNAVAILABLE-供应不上，PARKING_INSUFFICIENT-停放时间不足")
        private String warningType;

        @ApiModelProperty(value = "预警描述")
        private String description;
    }
}
