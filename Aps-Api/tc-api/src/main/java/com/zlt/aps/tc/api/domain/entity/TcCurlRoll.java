package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@ApiModel(value = "胎侧卷曲长度对象", description = "胎侧卷曲长度对象")
@Data
@TableName(value = "T_TC_CURL_ROLL")
public class TcCurlRoll extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.curlRoll.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tc.curlRoll.sidewallCode")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "胎侧编码", name = "sidewallCode")
    @TableField(value = "SIDEWALL_CODE")
    private String sidewallCode;

    @Excel(name = "ui.data.column.tc.curlRoll.curlLength")
    @ImportValidated(required = true, number = true, min = 0, max = 999999)
    @ApiModelProperty(value = "卷曲长度", name = "curlLength")
    @TableField(value = "CURL_LENGTH")
    private BigDecimal curlLength;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}