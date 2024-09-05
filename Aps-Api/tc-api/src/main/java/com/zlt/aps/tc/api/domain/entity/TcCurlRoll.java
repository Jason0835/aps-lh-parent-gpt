package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
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
 * 胎侧卷曲信息维护表
 * </p>
 *
 * @author zlt
 * @since 2023-09-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TC_CURL_ROLL")
@ApiModel(value = "TcCurlRoll对象", description = "胎侧卷曲信息维护表")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class TcCurlRoll extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "胎侧编号")
    @TableField("SIDEWALL_CODE")
    private String sidewallCode;

    @ApiModelProperty(value = "卷曲长度。此胎侧一卷的最大长度，单位：米。")
    @TableField(value = "CURL_LENGTH", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    @Excel(name = "ui.curlRoll.column.length")
    @ImportValidated(name = "ui.curlRoll.column.length", required = true, max = 999999, min = 0)
    private BigDecimal curlLength;
}
