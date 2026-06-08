package com.zlt.aps.dj.api.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.ibatis.type.JdbcType;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 垫胶卷曲信息维护表
 * </p>
 *
 * @author zlt
 * @since 2026-06-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_DJ_CURL_ROLL")
@ApiModel(value = "DjCurlRoll对象", description = "垫胶卷曲信息维护表")
public class DjCurlRoll extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "垫胶编号")
    @TableField("LINING_CODE")
    private String liningCode;

    @ApiModelProperty(value = "卷曲长度。此垫胶一卷的最大长度，单位：米。")
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
    
    @TableField(exist = false)
    private String orderStr;
}
