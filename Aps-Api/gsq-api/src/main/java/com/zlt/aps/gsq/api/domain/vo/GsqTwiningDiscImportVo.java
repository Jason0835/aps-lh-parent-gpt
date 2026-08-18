package com.zlt.aps.gsq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 钢丝圈缠绕盘导入对象（主子表平铺结构，非数据库表对象）
 * <p>导入模板按主子表平铺设计：主表字段（缠绕盘编号/名称/状态/英寸/数量/主表备注）+
 * 子表字段（钢丝圈编号/名称/明细备注）在同一行填写。</p>
 * <p>填写规则：同一缠绕盘对应多个钢丝圈时，主表字段重复填写多行，
 * 每行对应一条子表明细；缠绕盘主表字段以首行为准。</p>
 *
 * @author zlt
 * @date 2026-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "钢丝圈缠绕盘导入对象", description = "钢丝圈缠绕盘主子表平铺导入")
public class GsqTwiningDiscImportVo extends BaseEntity {

    /** 缠绕盘编号（主表，唯一键；同一缠绕盘多行明细时重复填写） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.twiningDiscCode")
    @ApiModelProperty(value = "缠绕盘编号", position = 10)
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String twiningDiscCode;

    /** 缠绕盘名称（主表） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.twiningDiscName")
    @ApiModelProperty(value = "缠绕盘名称", position = 20)
    @ImportValidated(required = true, maxLength = 100)
    private String twiningDiscName;

    /** 状态（主表，0正常 1停用，字典反显，不填默认0） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.status", dictType = "sys_normal_disable")
    @ApiModelProperty(value = "状态", position = 30)
    private String status;

    /** 英寸（主表） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.proSize")
    @ApiModelProperty(value = "英寸", position = 40)
    @ImportValidated(required = true, number = true, min = 0, max = 9999.99)
    private BigDecimal proSize;

    /** 数量（主表） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.qty")
    @ApiModelProperty(value = "数量", position = 50)
    @ImportValidated(number = true, min = 0, max = 999999)
    private Integer qty;

    /** 主表备注（与子表明细备注区分） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.mainRemark")
    @ApiModelProperty(value = "主表备注", position = 60)
    @ImportValidated(maxLength = 900)
    private String mainRemark;

    /** 钢丝圈编号（子表明细，同一缠绕盘+同一钢丝圈不允许重复） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.steelRingCode")
    @ApiModelProperty(value = "钢丝圈编号", position = 70)
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String steelRingCode;

    /** 钢丝圈名称（子表明细；未填写时按编号从施工信息表反显） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.steelRingName")
    @ApiModelProperty(value = "钢丝圈名称", position = 80)
    @ImportValidated(maxLength = 30)
    private String steelRingName;

    /** 子表明细备注（与主表备注区分） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.subRemark")
    @ApiModelProperty(value = "明细备注", position = 90)
    @ImportValidated(maxLength = 900)
    private String subRemark;
}
