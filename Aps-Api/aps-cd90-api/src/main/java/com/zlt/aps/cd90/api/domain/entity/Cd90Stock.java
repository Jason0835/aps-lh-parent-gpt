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
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(value = "直裁库存管理", description = "直裁库存管理")
@TableName("t_cd90_stock")
public class Cd90Stock extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd90Stock.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 库存日期 */
    @ApiModelProperty("库存日期")
    @ImportExcelValidated(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("STOCK_DATE")
    @Excel(name = "ui.data.column.cd90Stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date stockDate;

    /** 班次 */
    @ApiModelProperty("班次")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("SHIFT_CODE")
    @Excel(name = "ui.data.column.cd90Stock.shiftCode", dictType = "class_num_three_plan")
    private String shiftCode;

    /** 库存快照同步时间 */
    @ApiModelProperty("库存快照同步时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("SNAPSHOT_TIME")
    @Excel(name = "ui.data.column.cd90Stock.snapshotTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date snapshotTime;

    /** 胎体代码 */
    @ApiModelProperty("胎体代码")
    @ImportExcelValidated(required = true, maxLength = 60)
    @TableField("MATERIAL_CODE")
    @Excel(name = "ui.data.column.cd90Stock.materialCode")
    private String materialCode;

    /** 库存量(米) */
    @ApiModelProperty("库存量(米)")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("STOCK_NUM")
    @Excel(name = "ui.data.column.cd90Stock.stockNum")
    private Double stockNum;

    /** 修正数量(米) */
    @ApiModelProperty("修正数量(米)")
    @TableField("MODIFY_NUM")
    @Excel(name = "ui.data.column.cd90Stock.modifyNum")
    private Double modifyNum;

    /** 不良数量(米) */
    @ApiModelProperty("不良数量(米)")
    @TableField("BAD_NUM")
    @Excel(name = "ui.data.column.cd90Stock.badNum")
    private Double badNum;

    /** 层数 */
    @ApiModelProperty("层数")
    @TableField("LAYERS")
    @Excel(name = "ui.data.column.cd90Stock.layers")
    private Integer layers;

    /** 数据来源：0-MES同步，1-人工维护 */
    @ApiModelProperty("数据来源：0-MES同步，1-人工维护")
    @TableField("DATA_SOURCE")
    @Excel(name = "ui.data.column.cd90Stock.dataSource", dictType = "lh_precision_data_source")
    private String dataSource;
}