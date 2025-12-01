package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 模具可用状态对象 t_mould_use_status
 *
 * @author leo
 * @date 2021-08-27
 */
@Data
@ApiModel(value = "模具可用状态对象", description = "模具可用状态对象 ")
@TableName(value = "T_MDM_MOULD_USE_STATUS")
public class MdmMouldUseStatus extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.mouldusestatus.year")
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份")
    @TableField(value = "YEAR")
    private Long year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.mouldusestatus.month")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份")
    @TableField(value = "MONTH")
    private Long month;

    /**
     * 可用分厂编号
     */
    @Excel(name = "ui.data.column.mouldusestatus.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "可用分厂编号")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 模具状态
     */
    @ImportExcelValidated(required = true, digits = true)
    @Excel(name = "ui.data.column.mouldusestatus.mouldStatus", dictType = "biz_available_status")
    @ApiModelProperty(value = "模具状态")
    @TableField(value = "MOULD_STATUS")
    private Long mouldStatus;
    /**
     * 模具大类（模具规格描述）
     */
    @Excel(name = "ui.data.column.modelinfo.spattern", width = 32.0D)
    @ApiModelProperty(value = "模具大类")
    @TableField(exist = false)
    private String specificationsPattern;

    /**
     * 模具号
     */
    @Excel(name = "ui.data.column.mouldusestatus.mouldCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 40)
    @ApiModelProperty(value = "模具号")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    /**
     * 归属分厂
     */
    @Excel(name = "ui.data.column.mouldusestatus.owerFactoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "归属分厂")
    @TableField(value = "OWER_FACTORY_CODE")
    private String owerFactoryCode;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.mouldusestatus.specifications", width = 50, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "规格")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.mouldusestatus.pattern", width = 40, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "花纹")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 模具类型
     */
    @Excel(name = "ui.data.column.mouldusestatus.mouldType", dictType = "biz_mould_Type")
    @ApiModelProperty(value = "模具类型")
    @TableField(value = "MOULD_TYPE")
    private String mouldType;

    @Excel(name = "ui.data.column.mouldusestatus.remark", width = 40, align = Excel.Align.LEFT)
    @ImportExcelValidated(maxLength = 1000)
    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;

    // 物料号
    @TableField(exist = false)
    private transient String productCode;

    @TableField(exist = false)
    private Integer isDelete;
}
