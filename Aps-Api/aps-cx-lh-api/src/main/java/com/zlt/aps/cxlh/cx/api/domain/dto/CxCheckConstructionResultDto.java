package com.zlt.aps.cxlh.cx.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import lombok.Data;

/**
 * 施工检查结果数据对象
 */
@Data
public class CxCheckConstructionResultDto {
    /**
     * 胎胚SAP
     */
    @Excel(name = "ui.data.column.productConstruction.sapCode")
    private String sapCode;
    /**
     * 胎胚号
     */
    @Excel(name = "ui.data.column.productConstruction.embryoCode")
    private String embryoCode;
    /**
     * 胎胚版本
     */
    @Excel(name = "ui.data.column.productConstruction.embryoVersion")
    private String embryoVersion;
    /**
     * 错误信息
     */
    @Excel(name = "ui.data.column.productConstruction.errorMessage")
    private String errorMessage;
}
