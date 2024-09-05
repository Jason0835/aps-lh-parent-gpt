package com.zlt.aps.template.lh;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 模具变动单对象 lh_mold_change_plan
 *
 * @author zlt
 * @date 2021-06-17
 */
@ApiModel(value = "模具变动单对象", description = "模具变动单对象 ")
@Data
public class LhMoldChangePlanTemp extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 模具变动单批次号
     */
    @ApiModelProperty(value = "模具变动单批次号", position = 20)
    private String moldBatchNo;

    /**
     * 硫化机台编号
     */
    @Excel(name = "ui.data.column.moldChange.lhMachineCode")
    @ApiModelProperty(value = "硫化机台编号", position = 30)
    private String lhMachineCode;


    /**
     * 前规格品号
     */
    @ImportValidated(maxLength = 1000, isCode = true)
    @Excel(name = "ui.data.column.moldChange.beforeSapCode")
    @ApiModelProperty(value = "前规格品号")
    private String beforeSapCode;

    /**
     * 前规格描述
     */
    @ImportValidated(maxLength = 1000)
    @Excel(name = "ui.data.column.moldChange.beforeSpecDesc")
    @ApiModelProperty(value = "前规格描述", position = 50)
    private String beforeSpecDesc;

    /**
     * 胎胚库存
     */
    @ImportValidated(number = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.moldChange.tireRoughStock")
    @ApiModelProperty(value = "胎胚库存", position = 60)
    private Integer tireRoughStock;

    /**
     * 变更类型：数据字典维护拆模换、点数换、合并收尾、拆模合并、左模收尾合并、右模收尾合并
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.moldChange.changeType", dictType = "CHANGE_TYPE")
    @ApiModelProperty(value = "变更类型", position = 70)
    private String changeType;

    /**
     * 后规格品号
     */
    @ImportValidated(maxLength = 1000, isCode = true)
    @Excel(name = "ui.data.column.moldChange.afterSapCode")
    @ApiModelProperty(value = "后规格品号", position = 80)
    private String afterSapCode;

    /**
     * 后规格描述
     */
    @ImportValidated(maxLength = 1000)
    @Excel(name = "ui.data.column.moldChange.afterSpecDesc")
    @ApiModelProperty(value = "后规格描述", position = 90)
    private String afterSpecDesc;

    /**
     * 库区信息,跟主计划库区同步,维护在数据字典中
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.cxScheduleResult.storageLocation", dictType = "STORAGE_LOCATION")
    @ApiModelProperty(value = "库存地点", position = 100)
    private String stockArea;

    /**
     * 更换时间
     */
    @ImportValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.moldChange.changeTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更换时间", position = 110)
    private Date changeTime;

    /**
     * 备注
     */
    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 120)
    private String remark;


    /**
     * 排程日期
     */
    @ImportValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd",sort = 0)
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;
}
