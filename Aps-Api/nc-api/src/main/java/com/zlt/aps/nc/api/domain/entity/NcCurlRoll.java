package com.zlt.aps.nc.api.domain.entity;

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
 * 内衬卷曲信息维护表
 * </p>
 *
 * @author zlt
 * @since 2023-09-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_CURL_ROLL")
@ApiModel(value = "NcCurlRoll对象", description = "内衬卷曲信息维护表")
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class NcCurlRoll extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "内衬编号")
    @TableField("LINING_CODE")
    private String liningCode;

    @ApiModelProperty(value = "卷曲长度。此内衬一卷的最大长度，单位：米。")
    @TableField(value = "CURL_LENGTH", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    @Excel(name = "ui.curlRoll.column.length")
    @ImportValidated(name = "ui.curlRoll.column.length", required = true, max = 999999, min = 0)
    private BigDecimal curlLength;

    /**
     * 查询编号，用于精确查询
     */
    @ApiModelProperty(value = "查询编号，用于精确查询")
    @TableField(exist = false)
    private String queryCode;
}
