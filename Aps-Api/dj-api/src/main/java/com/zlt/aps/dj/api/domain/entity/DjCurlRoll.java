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
import com.zlt.common.annotation.ImportExcelValidated;

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

    @ApiModelProperty(value = "垫胶代码")
    @Excel(name="ui.dj.curlRoll.column.paddingCode")
    @ImportExcelValidated(name = "ui.dj.curlRoll.column.paddingCode", required = true, isCode = true, maxLength = 20)
    @TableField("PADDING_CODE")
    private String paddingCode;

    @ApiModelProperty(value = "卷曲长度")
    @TableField(value = "CURL_LENGTH", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    @Excel(name = "ui.curlRoll.column.length")
    @ImportExcelValidated(name = "ui.curlRoll.column.length", required = true, max = 999999, min = 0)
    private BigDecimal curlLength;

    @Excel(name = "ui.data.column.info.remark")
    @ImportExcelValidated(name = "ui.data.column.info.remark", maxLength = 100)
    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;
}
