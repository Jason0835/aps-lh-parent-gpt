package com.zlt.aps.lh.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 不合格胶库存对象 t_mix_bad_rubber_stock
 * 
 * @author zlt
 * @date 2021-11-08
 */
@Data
@ApiModel(value = "不合格胶库存对象", description = "不合格胶库存对象 ")
public class MixBadRubberStock extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.badStock.factoryCode")
    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    /** 物料编号 */
    @Excel(name = "ui.data.column.badStock.materialCode")
    @ApiModelProperty(value = "物料编号")
    private String materialCode;

    /** 库存 */
    @Excel(name = "ui.data.column.badStock.stockNum")
    @ApiModelProperty(value = "库存")
    private BigDecimal stockNum;

    /** 库存日期 */
    @Excel(name = "ui.data.column.badStock.stockDate")
    @ApiModelProperty(value = "库存日期")
    private String stockDate;

    /** 删除标识 */
    private String delFlag;


}
