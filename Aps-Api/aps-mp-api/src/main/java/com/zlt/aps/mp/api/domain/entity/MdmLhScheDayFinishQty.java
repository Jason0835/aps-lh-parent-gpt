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
 * APS硫化排程日完成量接口
 *
 * @author APS Team
 * @since 2026/03/27
 */
@ApiModel(value = "APS硫化排程日完成量接口", description = "APS硫化排程日完成量接口")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_MES_LH_DAY_FINISH_QTY")
public class MdmLhScheDayFinishQty extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "完成日期")
    @TableField(value = "FINISH_DATE")
    private Date finishDate;

    @ApiModelProperty(value = "胚胎日完成量")
    @TableField(value = "DAY_FINISH_QTY")
    private BigDecimal dayFinishQty;

    @ApiModelProperty(value = "物料编码（NC）")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "物料编码（MES）")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

}
