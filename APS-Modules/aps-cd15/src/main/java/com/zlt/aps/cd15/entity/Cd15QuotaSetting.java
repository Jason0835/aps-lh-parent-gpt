package com.zlt.aps.cd15.entity;

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
 * 15度裁断定额设定表
 * </p>
 *
 * @author chen
 * @since 2021-06-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CD15_QUOTA_SETTING")
@ApiModel(value = "Cd15QuotaSetting对象", description = "15度裁断定额设定表")
//@KeySequence(value = "SEQ_QUOTA_SETTING",dbType = DbType.ORACLE)
public class Cd15QuotaSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_QUOTA_SETTING")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "钢带代码")
    @TableField(value = "STEEL_STRIP_CODE", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String steelStripCode;

    @ApiModelProperty(value = "机台id（对应T_CD15_MACHINE_INFO表id）")
    @TableField(value = "MACHINE_ID", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private Long machineId;

    @ApiModelProperty(value = "定额")
    @TableField("QUOTA")
    private BigDecimal quota;

}
