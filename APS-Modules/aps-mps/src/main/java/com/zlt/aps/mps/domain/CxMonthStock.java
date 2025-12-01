package com.zlt.aps.mps.domain;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * <p>
 * 每月1号早8库存数据，需要跟MES沟通是否可以提供
 * </p>
 *
 * @author chen
 * @since 2021-06-17
 */
@Data
@TableName("T_CX_MONTH_STOCK")
@ApiModel(value = "CxMonthStock对象", description = "成型月结库存表")
@KeySequence(value = "SEQ_CX_MONTHSTOCK",dbType = DbType.ORACLE)
public class CxMonthStock extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableField("ID")
    private Long id;

    @ApiModelProperty(value = "库存所属月份：yyyy-mm")
    @TableField("STOCK_MONTH")
    private Date stockMonth;

    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "库存量")
    @TableField("STOCK_NUM")
    private String stockNum;

    @ApiModelProperty(value = "超期库存")
    @TableField("OVER_TIME_STOCK")
    private String overTimeStock;

    @ApiModelProperty(value = "施工版本")
    @TableField("BOM_DATA_VERSION")
    private String bomDataVersion;
}
