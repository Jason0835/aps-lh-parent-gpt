package com.zlt.aps.template.gsq;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钢丝圈缠绕盘信息导入模板
 * <p>对应新表结构 T_GSQ_TWINING_DISC，用于导入时 Excel 字段映射</p>
 *
 * @author zlt
 * @date 2026-07-08
 */
@Data
@ApiModel(value = "钢丝圈缠绕盘信息导入模板", description = "钢丝圈缠绕盘信息导入模板")
public class GsqTwiningDiscTemp extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 缠绕盘编码（必填，唯一） */
    @ApiModelProperty(value = "缠绕盘编码")
    @Excel(name = "ui.data.column.gsq.twiningDisc.twiningDiscCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String twiningDiscCode;

    /** 缠绕盘名称（必填） */
    @ApiModelProperty(value = "缠绕盘名称")
    @Excel(name = "ui.data.column.gsq.twiningDisc.twiningDiscName")
    @ImportValidated(required = true, maxLength = 100)
    private String twiningDiscName;

    /** 状态（0正常 1停用） */
    @ApiModelProperty(value = "状态")
    @Excel(name = "ui.data.column.gsq.twiningDisc.status", dictType = "sys_normal_disable")
    private String status;

    /** 英寸（必填） */
    @ApiModelProperty(value = "英寸")
    @Excel(name = "ui.data.column.gsq.twiningDisc.proSize")
    @ImportValidated(required = true, number = true, min = 0, max = 9999.99)
    private BigDecimal proSize;

    /** 数量 */
    @ApiModelProperty(value = "数量")
    @Excel(name = "ui.data.column.gsq.twiningDisc.qty")
    @ImportValidated(number = true, min = 0, max = 999999)
    private Integer qty;

    /** 备注 */
    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 900)
    private String remark;
}
