package com.zlt.aps.cd15.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 钢压大卷信息维护表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CD15_BIG_ROLL")
@ApiModel(value = "Cd15BigRoll对象", description = "钢压大卷信息维护表")
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class Cd15BigRoll extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "钢压大卷编号")
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;

    @ApiModelProperty(value = "布卷长度。此钢压大卷一卷的最大长度，单位：米。")
    @TableField(value = "CLOTH_LENGTH", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private BigDecimal clothLength;

    @ApiModelProperty(value = "折合生产条数。一卷大概能生产的胎胚数量，单位：条。")
    @TableField(value = "CONVERT_PRODUCE_NUM", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.INTEGER)
    private Integer convertProduceNum;

    @ApiModelProperty(value = "实际卷取标准。此钢压大卷实际卷取的长度，单位：米。")
    @TableField(value = "ACT_CLOTH_LENGTH", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private BigDecimal actClothLength;
}
