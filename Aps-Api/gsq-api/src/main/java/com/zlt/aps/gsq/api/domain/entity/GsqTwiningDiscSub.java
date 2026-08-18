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

/**
 * 钢丝圈缠绕盘明细对象 T_GSQ_TWINING_DISC_SUB
 * <p>子表：存储缠绕盘关联的钢丝圈信息，通过 DISC_ID 关联主表</p>
 *
 * @author zlt
 * @date 2026-07-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_GSQ_TWINING_DISC_SUB")
@ApiModel(value = "钢丝圈缠绕盘明细对象", description = "钢丝圈缠绕盘明细")
public class GsqTwiningDiscSub extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 缠绕盘主表ID（关联 T_GSQ_TWINING_DISC.ID） */
    @ApiModelProperty(value = "缠绕盘主表ID", position = 10)
    @TableField("DISC_ID")
    private Long discId;

    /** 钢丝圈编号 */
    @Excel(name = "ui.data.column.gsq.twiningDisc.steelRingCode")
    @ApiModelProperty(value = "钢丝圈编号", position = 20)
    @TableField("STEEL_RING_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String steelRingCode;

    /** 钢丝圈名称（数据库字段，页面可录入；编辑回显时根据钢丝圈编号从施工信息表反显） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.steelRingName")
    @ApiModelProperty(value = "钢丝圈名称", position = 30)
    @TableField("STEEL_RING_NAME")
    private String steelRingName;

    /** 子表明细备注（与主表备注区分，使用独立多语言key） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.subRemark")
    @ApiModelProperty(value = "明细备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 900)
    private String remark;
}
