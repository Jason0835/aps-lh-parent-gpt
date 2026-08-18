package com.zlt.aps.gsq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 钢丝圈缠绕盘对象 T_GSQ_TWINING_DISC
 * <p>主表：存储缠绕盘基本信息（编码、名称、英寸、数量等）</p>
 *
 * @author zlt
 * @date 2026-07-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_GSQ_TWINING_DISC")
@ApiModel(value = "钢丝圈缠绕盘对象", description = "钢丝圈缠绕盘管理")
public class GsqTwiningDisc extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 缠绕盘编码（唯一） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.twiningDiscCode")
    @ApiModelProperty(value = "缠绕盘编码", position = 10)
    @TableField("TWINING_DISC_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String twiningDiscCode;

    /** 缠绕盘名称 */
    @Excel(name = "ui.data.column.gsq.twiningDisc.twiningDiscName")
    @ApiModelProperty(value = "缠绕盘名称", position = 20)
    @TableField("TWINING_DISC_NAME")
    @ImportValidated(required = true, maxLength = 100)
    private String twiningDiscName;

    /** 状态（0正常 1停用） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.status", dictType = "sys_normal_disable")
    @ApiModelProperty(value = "状态", position = 30)
    @TableField("STATUS")
    private String status;

    /** 英寸 */
    @Excel(name = "ui.data.column.gsq.twiningDisc.proSize")
    @ApiModelProperty(value = "英寸", position = 40)
    @TableField("PRO_SIZE")
    @ImportValidated(required = true, number = true, min = 0, max = 9999.99)
    private BigDecimal proSize;

    /** 数量 */
    @Excel(name = "ui.data.column.gsq.twiningDisc.qty")
    @ApiModelProperty(value = "数量", position = 50)
    @TableField("QTY")
    @ImportValidated(number = true, min = 0, max = 999999)
    private Integer qty;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 900)
    private String remark;

    /** 钢丝圈缠绕盘明细列表（非数据库字段，主子表编辑时使用） */
    @TableField(exist = false)
    private List<GsqTwiningDiscSub> subList;

    /** 排序字段（非数据库字段，用于列表动态排序，格式：字段名+排列方式） */
    @TableField(exist = false)
    private String orderStr;
}
