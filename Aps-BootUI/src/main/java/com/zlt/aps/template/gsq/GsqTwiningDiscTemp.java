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
 * <p>对应表结构 T_GSQ_TWINING_DISC，字段顺序与页面列表一致（不含数据来源、统计字段），用于导入时 Excel 字段映射</p>
 * <p>列顺序：工厂 → 缠绕盘编码 → 缠绕盘名称 → 英寸 → 钢丝排列方式 → 数量 → 状态 → 备注</p>
 *
 * @author zlt
 * @date 2026-07-08
 */
@Data
@ApiModel(value = "钢丝圈缠绕盘信息导入模板", description = "钢丝圈缠绕盘信息导入模板")
public class GsqTwiningDiscTemp extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂代码（必填） */
    @ApiModelProperty(value = "工厂代码")
    @Excel(name = "ui.data.column.gsq.twiningDisc.factoryCode", dictType = "biz_factory_name", sort = 10)
    @ImportValidated(required = true, maxLength = 50)
    private String factoryCode;

    /** 缠绕盘编码（必填，唯一） */
    @ApiModelProperty(value = "缠绕盘编码")
    @Excel(name = "ui.data.column.gsq.twiningDisc.twiningDiscCode", sort = 20)
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String twiningDiscCode;

    /** 缠绕盘名称（必填） */
    @ApiModelProperty(value = "缠绕盘名称")
    @Excel(name = "ui.data.column.gsq.twiningDisc.twiningDiscName", sort = 30)
    @ImportValidated(required = true, maxLength = 100)
    private String twiningDiscName;

    /** 英寸（必填） */
    @ApiModelProperty(value = "英寸")
    @Excel(name = "ui.data.column.gsq.twiningDisc.proSize", sort = 40)
    @ImportValidated(required = true, number = true, min = 0, max = 9999.99)
    private BigDecimal proSize;

    /** 钢丝排列方式（必填，如3-4-5-4-3） */
    @ApiModelProperty(value = "钢丝排列方式")
    @Excel(name = "ui.data.column.gsq.twiningDisc.sortType", sort = 50)
    @ImportValidated(required = true, maxLength = 50)
    private String sortType;

    /** 数量 */
    @ApiModelProperty(value = "数量")
    @Excel(name = "ui.data.column.gsq.twiningDisc.qty", sort = 60)
    @ImportValidated(number = true, min = 0, max = 999999)
    private Integer qty;

    /** 状态（0正常 1停用） */
    @ApiModelProperty(value = "状态")
    @Excel(name = "ui.data.column.gsq.twiningDisc.status", dictType = "sys_normal_disable", sort = 70)
    private String status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    @Excel(name = "ui.common.column.remark", sort = 80)
    @ImportValidated(maxLength = 900)
    private String remark;
}
