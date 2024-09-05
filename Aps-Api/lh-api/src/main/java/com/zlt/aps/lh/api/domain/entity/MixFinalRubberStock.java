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
 * 终炼胶库存对象 t_mix_final_rubber_stock
 * 
 * @author zlt
 * @date 2021-11-09
 */
@ApiModel(value = "终炼胶库存对象", description = "终炼胶库存对象 ")
@Data
public class MixFinalRubberStock extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.finalRubberStock.factoryCode")
    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    /** 物料编号 */
    @Excel(name = "ui.data.column.finalRubberStock.materialCode")
    @ApiModelProperty(value = "物料编号")
    private String materialCode;

    /** 库存 */
    @Excel(name = "ui.data.column.finalRubberStock.stockNum")
    @ApiModelProperty(value = "库存")
    private Long stockNum;

    /** 安全库存 */
    @Excel(name = "ui.data.column.finalRubberStock.safetyStockNum")
    @ApiModelProperty(value = "安全库存")
    private Long safetyStockNum;

    /** 库存日期 */
    @Excel(name = "ui.data.column.finalRubberStock.stockDate")
    @ApiModelProperty(value = "库存日期")
    private String stockDate;

    /** 删除标识 */
    @ApiModelProperty(value = "库存日期")
    private String delFlag;





}
