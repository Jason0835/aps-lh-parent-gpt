package com.zlt.aps.mp.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Chen
 * @since 2025/10/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmModelInfoExportVo extends BaseEntity {

    /**
     * 模具
     */
    @Excel(name = "ui.data.column.mdmModelInfo.mouldNo")
    @ApiModelProperty(value = "模具", name = "mouldNo")
    private String mouldNo;

    /**
     * 模具数
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.mouldQty")
    @ApiModelProperty(value = "模具数", name = "mouldNum")
    private Integer mouldNum;

    /**
     * 寸口范围
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.proSize")
    @ApiModelProperty(value = "寸口范围", name = "proSize")
    private String proSize;
}
