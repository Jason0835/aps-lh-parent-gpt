package com.zlt.aps.cd15.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;

import java.math.BigDecimal;

/** CD15固定生产计划模板导入明细。 */
@Data
public class Cd15ScheduleResultTemplateImportVO extends BaseEntity {
    /** 机台编号 */
    @ImportExcelValidated(required = true)
    @Excel(importName = "machineCode")
    private String machineCode;
    /** 单耗 */
    @Excel(importName = "unitConsume")
    private BigDecimal unitConsume;
    /** 计划余料量 */
    @Excel(importName = "planSurplusQty")
    private BigDecimal planSurplusQty;
    /** 钢带编码 */
    @ImportExcelValidated(required = true)
    @Excel(importName = "steelStripCode")
    private String steelStripCode;
    /** 裁断角度 */
    @ImportExcelValidated(required = true)
    @Excel(importName = "cuttingAngle")
    private String cuttingAngle;
    /** 大卷编码 */
    @ImportExcelValidated(required = true)
    @Excel(importName = "bigRollCode")
    private String bigRollCode;
    /** 库存量 */
    @Excel(importName = "stockQty")
    private Double stockQty;
    /** 储道编码 */
    @Excel(importName = "storageLaneCode")
    private String storageLaneCode;
    /** 一班计划量 */
    @Excel(importName = "class1PlanQty")
    private Double class1PlanQty;
    /** 一班完工量 */
    @Excel(importName = "class1FinishQty")
    private Double class1FinishQty;
    /** 二班计划量 */
    @Excel(importName = "class2PlanQty")
    private Double class2PlanQty;
    /** 二班完工量 */
    @Excel(importName = "class2FinishQty")
    private Double class2FinishQty;
    /** 三班计划量 */
    @Excel(importName = "class3PlanQty")
    private Double class3PlanQty;
    /** 三班完工量 */
    @Excel(importName = "class3FinishQty")
    private Double class3FinishQty;
    /** 成型机台编码（多个时逗号分隔） */
    @Excel(importName = "cxMachineCodes")
    private String cxMachineCodes;
}
