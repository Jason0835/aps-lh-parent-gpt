package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 芯片库存
 *
 * @author APS Team
 * @date 2026-04-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_MDM_CHIP_STOCK")
@ApiModel(description = "芯片库存")
public class MdmChipStock extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @TableField(value = "ID")
    @ApiModelProperty(value = "主键")
    private Long id;

    /**
     * 分公司
     */
    @Excel(name = "ui.data.column.mdmChipStock.companyCode", width = 20)
    @TableField(value = "COMPANY_CODE")
    @ApiModelProperty(value = "分公司")
    private String companyCode;

    /**
     * 分厂
     */
    @Excel(name = "ui.data.column.mdmChipStock.factoryCode", width = 20, dictType = "biz_factory_name")
    @TableField(value = "FACTORY_CODE")
    @ApiModelProperty(value = "分厂")
    private String factoryCode;

    /**
     * 芯片编号 - 芯片的唯一标识
     */
    @Excel(name = "ui.data.column.mdmChipStock.chipCode", width = 30)
    @TableField(value = "CHIP_CODE")
    @ApiModelProperty(value = "芯片编号 - 芯片的唯一标识")
    private String chipCode;

    /**
     * 库存量 - 芯片库存量
     */
    @Excel(name = "ui.data.column.mdmChipStock.stockNum", width = 20)
    @TableField(value = "STOCK_NUM")
    @ApiModelProperty(value = "库存量 - 芯片库存量")
    private Integer stockNum;

    /**
     * 完成量
     */
    @Excel(name = "ui.data.column.mdmChipStock.finishQty", width = 20)
    @TableField(value = "FINISH_QTY")
    @ApiModelProperty(value = "完成量")
    private Integer finishQty;

    /**
     * 版本号
     */
//    @Excel(name = "ui.data.column.mdmChipStock.dataVersion", width = 20)
    @TableField(value = "DATA_VERSION")
    @ApiModelProperty(value = "版本号")
    private String dataVersion;


    /**
     * 备注
     */
    @Excel(name = "ui.data.column.mdmChipStock.remark", width = 50)
    @TableField(value = "REMARK")
    @ApiModelProperty(value = "备注")
    private String remark;

    // ============== 非数据库字段 ==============
    /**
     * 剩余可用量 = 库存量 - 完成量 (虚字段，不保存数据库)
     */
    @TableField(exist = false)
    @Excel(name = "ui.data.column.mdmChipStock.remainStockNum", width = 20)
    @ApiModelProperty(value = "剩余可用量 = 库存量 - 完成量")
    private Integer remainStockNum;
}
