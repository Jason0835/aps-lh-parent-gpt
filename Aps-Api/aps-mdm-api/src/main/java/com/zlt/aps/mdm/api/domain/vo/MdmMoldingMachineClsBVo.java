package com.zlt.aps.mdm.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "基础数据-成型机类型子对象Vo", description = "基础数据-成型机类型子对象Vo")
@Data
public class MdmMoldingMachineClsBVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.mdmMoldingMachineClsB.moldingMachineClassCode")
    @ApiModelProperty(value = "成型机类别编码", name = "moldingMachineClassCode")
    @TableField(exist = false)
    private String moldingMachineClassCode;

    /** 寸口 */
    @Excel(name = "ui.data.column.mdmMoldingMachineClsB.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /** 默认定额产能 */
    @Excel(name = "ui.data.column.mdmMoldingMachineClsB.productionQuotaQty")
    @ApiModelProperty(value = "定额产能", name = "productionQuotaQty")
    @TableField(value = "PRODUCTION_QUOTA_QTY")
    private Integer productionQuotaQty;

    /**
     * 成型机一班剩余产能
     */
    @TableField(exist = false)
    private Integer class1RemainCapacity;

    /**
     * 成型机二班剩余产能
     */
    @TableField(exist = false)
    private Integer class2RemainCapacity;

    /**
     * 成型机三班剩余产能
     */
    @TableField(exist = false)
    private Integer class3RemainCapacity;

    /**
     * 成型机一班定额
     */
    @TableField(exist = false)
    private Integer class1MachineQty;

    /**
     * 成型机两班定额
     */
    @TableField(exist = false)
    private Integer class2MachineQty;

    /**
     * 成型机三班定额
     */
    @TableField(exist = false)
    private Integer class3MachineQty;


}
