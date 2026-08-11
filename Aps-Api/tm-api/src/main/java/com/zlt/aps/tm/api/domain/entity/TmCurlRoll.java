package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@ApiModel(value = "胎面卷曲长度对象", description = "胎面卷曲长度对象")
@Data
@TableName(value = "T_TM_CURL_ROLL")
public class TmCurlRoll extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.curlRoll.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.curlRoll.treadCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "胎面编码", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    @Excel(name = "ui.data.column.tm.curlRoll.curlLength")
    @ImportExcelValidated(required = true, number = true, min = 0, max = 999999)
    @ApiModelProperty(value = "卷曲长度", name = "curlLength")
    @TableField(value = "CURL_LENGTH")
    private BigDecimal curlLength;

    @Excel(name = "ui.common.column.remark")
    @ImportExcelValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
