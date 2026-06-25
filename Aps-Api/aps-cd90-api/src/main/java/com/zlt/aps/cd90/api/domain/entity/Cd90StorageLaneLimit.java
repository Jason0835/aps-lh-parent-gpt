package com.zlt.aps.cd90.api.domain.entity;

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
@ApiModel(value = "直裁库排限制", description = "直裁库排限制")
@TableName("t_cd90_storage_lane_limit")
public class Cd90StorageLaneLimit extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @ApiModelProperty("工厂编码")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd90StorageLaneLimit.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;
    /**
     * 日期
     */
    @ApiModelProperty("日期")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("LANE_DATE")
    @Excel(name = "ui.data.column.cd90StorageLaneLimit.laneDate", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date laneDate;

    /**
     * 胎体代码/规格
     */
    @ApiModelProperty("胎体代码/规格")
    @ImportExcelValidated(required = false, maxLength = 60)
    @TableField("MATERIAL_CODE")
    @Excel(name = "ui.data.column.cd90StorageLaneLimit.materialCode")
    private String materialCode;

    /**
     * 班次
     */
    @ApiModelProperty("班次")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("SHIFT_CODE")
    @Excel(name = "ui.data.column.cd90StorageLaneLimit.shiftCode", dictType = "class_num_three_plan")
    private String shiftCode;
    /**
     * 库排号
     */
    @ApiModelProperty("库排号")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("STORAGE_LANE_CODE")
    @Excel(name = "ui.data.column.cd90StorageLaneLimit.storageLaneCode")
    private String storageLaneCode;

    /**
     * 当前车数
     */
    @ApiModelProperty("当前车数")
    @ImportExcelValidated(required = true)
    @TableField("CAR_NUM")
    @Excel(name = "ui.data.column.cd90StorageLaneLimit.carNum")
    private Integer carNum;
    /**
     * 最大车数
     */
    @ApiModelProperty("最大车数")
    @ImportExcelValidated(required = true)
    @TableField("MAX_CAR_NUM")
    @Excel(name = "ui.data.column.cd90StorageLaneLimit.maxCarNum")
    private Integer maxCarNum;
    /**
     * 剩余可用车数
     */
    @ApiModelProperty("剩余可用车数")
    @TableField("AVAILABLE_CAR_NUM")
//    @Excel(name = "ui.data.column.cd90StorageLaneLimit.availableCarNum")
    private Integer availableCarNum;
    /**
     * 数据来源
     */
    @ApiModelProperty("数据来源")
    @TableField("DATA_SOURCE")
//    @Excel(name = "ui.data.column.cd90StorageLaneLimit.dataSource")
    private String dataSource;
    /**
     * MES同步时间
     */
    @ApiModelProperty("MES同步时间")
    @TableField("MES_SYNC_TIME")
//    @Excel(name = "ui.data.column.cd90StorageLaneLimit.mesSyncTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date mesSyncTime;
}