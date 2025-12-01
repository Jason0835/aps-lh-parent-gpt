package com.zlt.aps.cd90.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

import java.math.BigDecimal;

/**
 * <p>
 * 90度裁断定额设定
 * </p>
 *
 * @author chen
 * @since 2021-06-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CD90_QUOTA_SETTING")
@ApiModel(value = "Cd90QuotaSetting对象", description = "90度裁断定额设定")
//@KeySequence(value = "SEQ_QUOTA_SETTING",dbType = DbType.ORACLE)
public class Cd90QuotaSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_QUOTA_SETTING")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "帘布代码")
    @TableField(value = "CLOTH_CODE", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String clothCode;

    @ApiModelProperty(value = "机台id（对应T_CD90_MACHINE_INFO表id）")
    @TableField(value = "MACHINE_ID", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private Long machineId;

    @ApiModelProperty(value = "定额")
    @TableField("QUOTA")
    private BigDecimal quota;
}
