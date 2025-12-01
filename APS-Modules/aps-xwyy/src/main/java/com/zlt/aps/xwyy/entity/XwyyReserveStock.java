package com.zlt.aps.xwyy.entity;

import java.math.BigDecimal;

import org.apache.ibatis.type.JdbcType;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.aps.common.core.domain.ApsBaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 纤维压延预生产库存倍数配置
 * </p>
 *
 * @author hak
 * @since 2025-02-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_XWYY_RESERVE_STOCK")
@ApiModel(value = "XwyyReserveStock对象", description = "预生产库存倍数配置")
public class XwyyReserveStock extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "纤维大卷编号")
    @TableField(value = "BIG_ROLL_CODE", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String bigRollCode;

    /** 预生产库存倍数 */
    @ApiModelProperty(value = "预生产库存倍数")
    @TableField("RESERVE_STOCK_RATE")
    private BigDecimal reserveStockRate;

}
