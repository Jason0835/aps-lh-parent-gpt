package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(value = "15度斜裁库排限制", description = "15度斜裁库排限制")
@TableName("t_cd15_storage_lane_limit")
public class Cd15StorageLaneLimit extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("工厂编码")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15StorageLaneLimit.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    @ApiModelProperty("日期")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("LANE_DATE")
    @Excel(name = "ui.data.column.cd15StorageLaneLimit.laneDate", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date laneDate;

    @ApiModelProperty("物料编码/钢带代码")
    @ImportExcelValidated(required = false, maxLength = 60)
    @TableField("MATERIAL_CODE")
    @Excel(name = "ui.data.column.cd15StorageLaneLimit.materialCode")
    private String materialCode;

    @ApiModelProperty("班次")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("SHIFT_CODE")
    @Excel(name = "ui.data.column.cd15StorageLaneLimit.shiftCode", dictType = "class_num_three_plan")
    private String shiftCode;

    @ApiModelProperty("库排号")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("STORAGE_LANE_CODE")
    @Excel(name = "ui.data.column.cd15StorageLaneLimit.storageLaneCode")
    private String storageLaneCode;

    @ApiModelProperty("当前车数")
    @ImportExcelValidated(required = true)
    @TableField("CAR_NUM")
    @Excel(name = "ui.data.column.cd15StorageLaneLimit.carNum")
    private Integer carNum;

    @ApiModelProperty("最大车数")
    @ImportExcelValidated(required = true)
    @TableField("MAX_CAR_NUM")
    @Excel(name = "ui.data.column.cd15StorageLaneLimit.maxCarNum")
    private Integer maxCarNum;

    @ApiModelProperty("剩余可用车数")
    @TableField("AVAILABLE_CAR_NUM")
    private Integer availableCarNum;

    @ApiModelProperty("数据来源")
    @TableField("DATA_SOURCE")
    private String dataSource;

    @ApiModelProperty("MES同步时间")
    @TableField("MES_SYNC_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date mesSyncTime;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
