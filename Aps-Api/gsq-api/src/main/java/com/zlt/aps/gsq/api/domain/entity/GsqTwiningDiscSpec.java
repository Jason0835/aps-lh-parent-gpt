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
 * 钢丝圈缠绕盘-规格关系对象 T_GSQ_TWINING_DISC_SPEC
 * <p>维护缠绕盘与钢丝圈规格的对应关系（多对多，与机台关系表T_GSQ_TWINING_DISC_MACHINE对称，
 * 统一以TWINING_DISC_CODE编码关联主表），独立页面管理，
 * 为MES同步（MES_WIRE_DISC_SPEC_MAPPING）及自动排程机台分配提供数据基础</p>
 *
 * @author zlt
 * @date 2026-08-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_GSQ_TWINING_DISC_SPEC")
@ApiModel(value = "钢丝圈缠绕盘-规格关系对象", description = "钢丝圈缠绕盘规格关系管理")
public class GsqTwiningDiscSpec extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 缠绕盘编码（关联T_GSQ_TWINING_DISC.TWINING_DISC_CODE） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.twiningDiscCode")
    @ApiModelProperty(value = "缠绕盘编码", position = 10)
    @TableField("TWINING_DISC_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String twiningDiscCode;

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

    /** 状态：0-启用，1-停用（单条规格关系独立启停） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.subStatus", dictType = "sys_normal_disable")
    @ApiModelProperty(value = "状态", position = 40)
    @TableField("STATUS")
    private String status;

    /** 工厂代码 */
    @ApiModelProperty(value = "工厂代码", position = 50)
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** MES数据版本号（MES同步字段，手工数据为空） */
    @ApiModelProperty(value = "MES数据版本号", position = 60)
    @TableField("DATA_VERSION")
    private String dataVersion;

    /** 数据来源（字典lh_precision_data_source）：0-MES同步，1-手工维护 */
    @Excel(name = "ui.data.column.gsq.twiningDisc.dataSource", dictType = "lh_precision_data_source")
    @ApiModelProperty(value = "数据来源（0-MES同步，1-手工维护）", position = 70)
    @TableField("DATA_SOURCE")
    private String dataSource;

    /** 备注 */
    @Excel(name = "ui.data.column.gsq.twiningDisc.subRemark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 900)
    private String remark;

    /** 缠绕盘名称（非数据库字段，列表/导出反显用，按缠绕盘编码关联主表反显） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.twiningDiscName")
    @ApiModelProperty(value = "缠绕盘名称（反显）", position = 510)
    @TableField(exist = false)
    private String twiningDiscName;

    /** 英寸（非数据库字段，列表/导出反显用，按缠绕盘编码关联主表反显） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.proSize")
    @ApiModelProperty(value = "英寸（反显）", position = 520)
    @TableField(exist = false)
    private String proSize;

    /** 钢丝排列方式（非数据库字段，列表/导出反显用，按缠绕盘编码关联主表反显） */
    @Excel(name = "ui.data.column.gsq.twiningDisc.sortType")
    @ApiModelProperty(value = "钢丝排列方式（反显）", position = 530)
    @TableField(exist = false)
    private String sortType;

    /** 排序字段（非数据库字段，用于列表动态排序，格式：字段名+排列方式） */
    @TableField(exist = false)
    private String orderStr;
}
