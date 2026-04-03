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
     * 分公司
     */
    // @Excel(name = "ui.data.column.mdmChipStock.companyCode")
    @TableField(value = "COMPANY_CODE")
    @ApiModelProperty(value = "分公司")
    private String companyCode;

    /**
     * 分厂
     */
    @Excel(name = "ui.data.column.mdmChipStock.factoryCode", dictType = "biz_factory_name")
    @TableField(value = "FACTORY_CODE")
    @ApiModelProperty(value = "分厂")
    private String factoryCode;

    /**
     * 芯片编号 - 芯片的唯一标识
     */
    @Excel(name = "ui.data.column.mdmChipStock.chipCode")
    @TableField(value = "CHIP_CODE")
    @ApiModelProperty(value = "芯片编号 - 芯片的唯一标识")
    private String chipCode;

    /**
     * 库存量 - 芯片库存量
     */
    @Excel(name = "ui.data.column.mdmChipStock.stockNum")
    @TableField(value = "STOCK_NUM")
    @ApiModelProperty(value = "库存量 - 芯片库存量")
    private Integer stockNum;

    /**
     * 完成量
     */
    @Excel(name = "ui.data.column.mdmChipStock.finishQty")
    @TableField(value = "FINISH_QTY")
    @ApiModelProperty(value = "完成量")
    private Integer finishQty;
    
    // ============== 非数据库字段 ==============
    /**
     * 剩余可用量 = 库存量 - 完成量 (虚字段，不保存数据库)
     */
    @TableField(exist = false)
    @Excel(name = "ui.data.column.mdmChipStock.remainStockNum")
    @ApiModelProperty(value = "剩余可用量 = 库存量 - 完成量")
    private Integer remainStockNum;

    /**
     * 版本号
     */
//    @Excel(name = "ui.data.column.mdmChipStock.dataVersion")
    @TableField(value = "DATA_VERSION")
    @ApiModelProperty(value = "版本号")
    private String dataVersion;


    /**
     * 备注
     */
    @Excel(name = "ui.data.column.mdmChipStock.remark")
    @TableField(value = "REMARK")
    @ApiModelProperty(value = "备注")
    private String remark;


}
