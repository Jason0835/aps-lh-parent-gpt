package com.zlt.aps.tc.api.domain.entity;

import com.zlt.aps.common.core.annotation.ImportValidated;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 胎侧代码前缀颜色设定对象 t_tc_sidewall_code_color
 * 
 * @author zlt
 * @date 2022-01-14
 */
@ApiModel(value = "胎侧代码前缀颜色设定对象", description = "胎侧代码前缀颜色设定对象 ")
@Data
public class TcSidewallCodeColor extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 胎侧代码 */
    @Excel(name = "ui.data.column.sidewallCodeColor.sidewallCode")
    @ImportValidated(required = true, maxLength = 20,isCode = true)
    @ApiModelProperty(value = "胎侧代码")
    private String sidewallCode;

    /** 匹配类型 */
    @ImportValidated(required = true, maxLength = 1)
    @Excel(name = "ui.data.column.sidewallCodeColor.matchType",dictType = "match_type")
    @ApiModelProperty(value = "匹配类型")
    private String matchType;

    /** 颜色类型 */
    @ImportValidated(required = true, maxLength = 1)
    @Excel(name = "ui.data.column.sidewallCodeColor.colorType",dictType = "BIG_ROLL_COLOR")
    @ApiModelProperty(value = "颜色类型")
    private String colorType;

    /** 颜色代码 */
    @ImportValidated(colorCode = true, maxLength = 10)
    @Excel(name = "ui.data.column.sidewallCodeColor.colorCode")
    @ApiModelProperty(value = "颜色代码")
    private String colorCode;

    /** 状态 */
    @Excel(name = "ui.data.column.sidewallCodeColor.status",dictType="STATUS")
    @ImportValidated(required = true,maxLength = 2)
    @ApiModelProperty(value = "状态")
    private String status;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(maxLength = 300)
    private String remark;

    /** 正则表达式 */
    @ApiModelProperty(value = "正则表达式")
    private String regularExpression;

    /** 删除标识：0--正常，1-删除 */
    private String delFlag;







}
