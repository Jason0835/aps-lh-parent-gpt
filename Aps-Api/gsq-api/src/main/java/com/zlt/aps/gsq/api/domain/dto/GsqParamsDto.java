package com.zlt.aps.gsq.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 钢丝圈排程参数配置 DTO
 *
 * @author chenxueyuan
 * @since 2021-06-04
 */
@Data
@ApiModel(value = "GsqParamsDto对象", description = "钢丝圈排程参数配置")
public class GsqParamsDto extends BaseEntity {

    private static final long serialVersionUID = 1110056585123675863L;

    /** 工厂编号 */
    @Excel(name = "ui.data.column.gsq.params.factoryCode", dictType = "biz_factory_name")
    @NotBlank(message = "工厂编号不能为空")
    @ApiModelProperty(value = "工厂编号", position = 10)
    private String factoryCode;

    /** 参数编码 */
    @Excel(name = "ui.data.column.gsq.params.paramCode")
    @NotBlank(message = "参数编码不能为空")
    @ApiModelProperty(value = "参数编码", position = 20)
    private String paramCode;

    /** 参数名称 */
    @Excel(name = "ui.data.column.gsq.params.paramName")
    @ApiModelProperty(value = "参数名称", position = 30)
    private String paramName;

    /** 参数值 */
    @Excel(name = "ui.data.column.gsq.params.paramValue")
    @ApiModelProperty(value = "参数值", position = 40)
    private String paramValue;

    /** 默认值 */
    @Excel(name = "ui.data.column.gsq.params.defaultValue")
    @ApiModelProperty(value = "默认值", position = 50)
    private String defaultValue;

    /** 参数分组 */
    @Excel(name = "ui.data.column.gsq.params.paramGroup")
    @ApiModelProperty(value = "参数分组", position = 60)
    private String paramGroup;

    /** 参数值类型 */
    @Excel(name = "ui.data.column.gsq.params.valueType")
    @ApiModelProperty(value = "参数值类型", position = 70)
    private String valueType;

    /** 校验正则表达式 */
    @ApiModelProperty(value = "校验正则表达式", position = 80)
    private String regularExpression;

    /** 正则校验失败后的错误提示 */
    @ApiModelProperty(value = "正则校验失败后的错误提示", position = 90)
    private String errorTips;

    /** 是否启用 */
    @Excel(name = "ui.data.column.gsq.params.enableStatus", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否启用", position = 100)
    private String enableStatus;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
