package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 胎圈备库班数配置对象 t_tq_stock_shift_config
 *
 * @author zlt
 * @date 2026-06-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TQ_STOCK_SHIFT_CONFIG")
@ApiModel(value = "胎圈备库班数配置对象", description = "胎圈备库班数配置对象")
public class TqStockShiftConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;
//
//    @ApiModelProperty(value = "主键ID")
//    @TableId(value = "ID", type = IdType.AUTO)
//    private Long id;

    @Excel(name = "ui.data.column.factoryCode", dictType = "biz_factory_name", sort = 10)
    @ApiModelProperty(value = "分厂编码", position = 20)
    @TableField("FACTORY_CODE")
    @ImportValidated(required = true, maxLength = 50)
    private String factoryCode;

    @Excel(name = "ui.data.column.stockShiftConfig.machineRange", dictType = "machine_range", sort = 20)
    @ApiModelProperty(value = "机台范围（字典 machine_range：LT/LE/EQ/GE/GT）", position = 30)
    @TableField("MACHINE_RANGE")
    @ImportValidated(required = true, maxLength = 10)
    private String machineRange;

    @Excel(name = "ui.data.column.stockShiftConfig.machineCount", sort = 30)
    @ApiModelProperty(value = "成型机台数", position = 40)
    @TableField("MACHINE_COUNT")
    @ImportValidated(required = true, number = true, min = 1, max = 999)
    private Integer machineCount;

    @Excel(name = "ui.data.column.stockShiftConfig.shiftCount", sort = 40)
    @ApiModelProperty(value = "备库班次数", position = 50)
    @TableField("SHIFT_COUNT")
    @ImportValidated(required = true, number = true, min = 1, max = 99)
    private Integer shiftCount;

//    @Excel(name = "ui.common.column.remark", sort = 50)
//    @ApiModelProperty(value = "备注", position = 500)
//    @TableField("REMARK")
//    @ImportValidated(maxLength = 500)
//    private String remark;
//
//    @ApiModelProperty(value = "删除标识", position = 600)
//    @TableLogic(value = "0", delval = "1")
//    @TableField("IS_DELETE")
//    private Integer isDelete;
}
