package com.zlt.aps.monthplan.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanNoticeOrderExcelTemplateVo.java
 * 描    述：月计划调整通知单导入模板下载对象
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-21
 */

@Data
@ApiModel(value = "月计划调整通知单导入模板下载对象", description = "月计划调整通知单导入模板下载对象")
public class MonthPlanNoticeOrderExcelTemplateVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.year")
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.month")
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.productCode")
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.productDesc")
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    private String productDesc;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.channel", dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道", name = "channel")
    private String channel;

    /**
     * 计划需求量
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.needQty")
    @ApiModelProperty(value = "计划需求量", name = "needQty")
    private Long needQty;


    /**
     * 备注
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.remark")
    @ApiModelProperty(value = "备注", name = "remark")
    private String remark;
}