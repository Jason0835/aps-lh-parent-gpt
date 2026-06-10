package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 直裁卷曲长度。
 */
@Data
@ApiModel(value = "直裁卷曲长度", description = "直裁卷曲长度")
@TableName("t_cd90_curl_length")
public class Cd90CurlLength extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd90CurlLength.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 帘布代号 */
    @ApiModelProperty(value = "帘布代号", name = "clothCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("CLOTH_CODE")
    @Excel(name = "ui.data.column.cd90CurlLength.clothCode")
    private String clothCode;

    /** 标准卷曲长度 */
    @ApiModelProperty(value = "标准卷曲长度", name = "curlLength")
    @ImportExcelValidated(required = true, maxLength = 10,number = true)
    @TableField("CURL_LENGTH")
    @Excel(name = "ui.data.column.cd90CurlLength.curlLength")
    private Double curlLength;
}