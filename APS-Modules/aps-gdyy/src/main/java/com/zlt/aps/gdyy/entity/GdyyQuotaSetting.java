package com.zlt.aps.gdyy.entity;

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
 * 钢带压延定额设定
 * </p>
 *
 * @author chen
 * @since 2021-06-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_GDYY_QUOTA_SETTING")
@ApiModel(value = "GdyyQuotaSetting对象", description = "钢带压延定额设定")
@KeySequence(value = "SEQ_QUOTA_SETTING", clazz = Long.class)
public class GdyyQuotaSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "钢带大卷编号")
    @TableField(value = "BIG_ROLL_CODE", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String bigRollCode;

    @ApiModelProperty(value = "定额")
    @TableField("QUOTA")
    private BigDecimal quota;

}
