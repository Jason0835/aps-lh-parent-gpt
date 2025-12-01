package com.zlt.aps.monthplan.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 模具可用状态-导入模板
 */
@Data
public class MdmMouldUseStatusTemplateVo extends BaseEntity {
    
    /**
     * 年份
     */
    @Excel(name = "ui.data.column.mouldusestatus.year")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 9999)
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
     * 模具号
     */
    @Excel(name = "ui.data.column.mouldusestatus.mouldCode")
    @ImportExcelValidated(required = true, isCode = true)
    @ApiModelProperty(value = "模具号")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    @Excel(name = "ui.data.column.mouldusestatus.remark", width = 40, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;
}
