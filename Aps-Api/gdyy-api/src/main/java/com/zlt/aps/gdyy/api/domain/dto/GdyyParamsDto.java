package com.zlt.aps.gdyy.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 钢带压延工序参数设置实体
 * @author steve
 */
@Data
@ApiModel(value="GdyyParams对象", description="钢带压延参数信息")
public class GdyyParamsDto extends ApsBaseDto {

    private static final long serialVersionUID = 1110056585123675863L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    /** 参数code */
    @Excel(name = "ui.data.column.paramsCode")
    @NotBlank(message = "参数代码不能为空")
    @ApiModelProperty(value = "参数code", position = 20)
    private String paramCode;

    /** 参数名称 */
    @Excel(name = "ui.data.column.paramsName")
    @ApiModelProperty(value = "参数名称", position = 30)
    private String paramName;

    /** 参数值 */
    @Excel(name = "ui.data.column.paramsValue")
    @ApiModelProperty(value = "参数值", position = 40)
    private String paramValue;

    /** 参数值对应的正则表达式 */
    @ApiModelProperty(value = "参数值对应的正则表达式", position = 50)
    private String regularExpression;

    /** 参数值根据正则表达式校验是失败后的错误提示 */
    @ApiModelProperty(value = "参数值根据正则表达式校验是失败后的错误提示", position = 60)
    private String errorTips;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
