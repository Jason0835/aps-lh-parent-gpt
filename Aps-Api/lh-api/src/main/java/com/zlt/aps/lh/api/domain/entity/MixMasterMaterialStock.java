package com.zlt.aps.lh.api.domain.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 母炼胶小料库存对象 t_mix_master_material_stock
 * 
 * @author zlt
 * @date 2021-11-09
 */
@ApiModel(value = "母炼胶小料库存对象", description = "母炼胶小料库存对象 ")
@Data
public class MixMasterMaterialStock extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.masterMaterialStock.factoryCode")
    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    /** 物料编号 */
    @Excel(name = "ui.data.column.masterMaterialStock.materialCode")
    @ApiModelProperty(value = "物料编号")
    private String materialCode;

    /** 库存 */
    @Excel(name = "ui.data.column.masterMaterialStock.stockNum")
    @ApiModelProperty(value = "库存")
    private Long stockNum;

    /** 库存日期 */
    @Excel(name = "ui.data.column.masterMaterialStock.stockDate")
    @ApiModelProperty(value = "库存日期")
    private String stockDate;

    /** 删除标识 */
    @ApiModelProperty(value = "库存日期")
    private String delFlag;





}
