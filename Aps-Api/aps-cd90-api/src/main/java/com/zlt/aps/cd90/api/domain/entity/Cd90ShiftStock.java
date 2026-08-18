package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * CD90班次开始库存。
 */
@Data
@TableName("t_cd90_shift_stock")
public class Cd90ShiftStock extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** 班次开始自然日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField("STOCK_DATE")
    private Date stockDate;

    /** 标准物理班次编码：01夜班、02早班、03中班。 */
    @TableField("SHIFT_CODE")
    private String shiftCode;

    /** 班次开始时间，用于消除夜班跨日歧义。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("SHIFT_START_TIME")
    private Date shiftStartTime;

    /** 库存物料编号（帘布代码）。 */
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /** 库存量，单位米。 */
    @TableField("STOCK_NUM")
    private Double stockNum;

    /** 修正数量，单位米。 */
    @TableField("MODIFY_NUM")
    private Double modifyNum;

    /** 不良数量，单位米。 */
    @TableField("BAD_NUM")
    private Double badNum;

    /** 上游快照生成时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("SNAPSHOT_TIME")
    private Date snapshotTime;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
