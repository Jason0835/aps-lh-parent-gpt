package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎面库存实体
 * 对应表 T_MDM_TREAD_STOCK
 */
@ApiModel(value = "胎面库存", description = "胎面库存")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_TREAD_STOCK")
public class MdmTreadStock extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "库存日期")
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    @ApiModelProperty(value = "胎面物料编码")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "可用库存")
    @TableField(value = "AVAILABLE_STOCK")
    private BigDecimal availableStock;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "分厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;
}
