package com.zlt.aps.cx.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 成型排程结果模板导入行数据。
 * <p>用于承接固定模板 cxjhtemplate.xls 中的一行明细数据，避免把多级表头映射到数据库实体。</p>
 *
 * @author APS Team
 */
@Data
public class CxScheduleResultTemplateImportVO extends BaseEntity {

    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.cxScheduleResult.cxMachineCode")
    private String cxMachineCode;

    @Excel(name = "ui.data.column.cxScheduleResult.cxMachineName")
    private String cxMachineName;

    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.cxScheduleResult.embryoCode")
    private String embryoCode;

    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.cxScheduleResult.materialCode")
    private String materialCode;

    @Excel(name = "ui.data.column.cxScheduleResult.materialDesc")
    private String materialDesc;

    @Excel(name = "ui.data.column.cxScheduleResult.mainMaterialDesc")
    private String mainMaterialDesc;

    @Excel(name = "ui.data.column.cxScheduleResult.structureName")
    private String structureName;

    @Excel(name = "ui.data.column.cxScheduleResult.bomDataVersion")
    private String bomDataVersion;

    @Excel(name = "ui.data.column.cxScheduleResult.orderNo")
    private String orderNo;

    @Excel(name = "ui.data.column.cxScheduleResult.cxBatchNo")
    private String cxBatchNo;

    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.cxScheduleResult.scheduleDate", dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduleDate;

    @Excel(name = "ui.data.column.cxScheduleResult.isRelease", dictType = "IS_RELEASE")
    private String isRelease;

    @Excel(name = "ui.data.column.cxScheduleResult.dataSource")
    private String dataSource;

    // ===== class1 =====
    @Excel(name = "ui.data.column.cxScheduleResult.class1PlanQty")
    private BigDecimal class1PlanQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class1FinishQty")
    private BigDecimal class1FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class1Analysis")
    private String class1Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class1RecipeType", dictType = "trial_status")
    private String class1RecipeType;

    @Excel(name = "ui.data.column.cxScheduleResult.class1RecipeNo")
    private String class1RecipeNo;

    // ===== class2 =====
    @Excel(name = "ui.data.column.cxScheduleResult.class2PlanQty")
    private BigDecimal class2PlanQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class2FinishQty")
    private BigDecimal class2FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class2Analysis")
    private String class2Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class2RecipeType", dictType = "trial_status")
    private String class2RecipeType;

    @Excel(name = "ui.data.column.cxScheduleResult.class2RecipeNo")
    private String class2RecipeNo;

    // ===== class3 =====
    @Excel(name = "ui.data.column.cxScheduleResult.class3PlanQty")
    private BigDecimal class3PlanQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class3FinishQty")
    private BigDecimal class3FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class3Analysis")
    private String class3Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class3RecipeType", dictType = "trial_status")
    private String class3RecipeType;

    @Excel(name = "ui.data.column.cxScheduleResult.class3RecipeNo")
    private String class3RecipeNo;

    // ===== class4 =====
    @Excel(name = "ui.data.column.cxScheduleResult.class4PlanQty")
    private BigDecimal class4PlanQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class4FinishQty")
    private BigDecimal class4FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class4Analysis")
    private String class4Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class4RecipeType", dictType = "trial_status")
    private String class4RecipeType;

    // ===== class5 =====
    @Excel(name = "ui.data.column.cxScheduleResult.class5PlanQty")
    private BigDecimal class5PlanQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class5FinishQty")
    private BigDecimal class5FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class5Analysis")
    private String class5Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class5RecipeType", dictType = "trial_status")
    private String class5RecipeType;

    // ===== class6 =====
    @Excel(name = "ui.data.column.cxScheduleResult.class6PlanQty")
    private BigDecimal class6PlanQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class6FinishQty")
    private BigDecimal class6FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class6Analysis")
    private String class6Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class6RecipeType", dictType = "trial_status")
    private String class6RecipeType;

    // ===== class7 =====
    @Excel(name = "ui.data.column.cxScheduleResult.class7PlanQty")
    private BigDecimal class7PlanQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class7FinishQty")
    private BigDecimal class7FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class7Analysis")
    private String class7Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class7RecipeType", dictType = "trial_status")
    private String class7RecipeType;

    // ===== class8 =====
    @Excel(name = "ui.data.column.cxScheduleResult.class8PlanQty")
    private BigDecimal class8PlanQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class8FinishQty")
    private BigDecimal class8FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class8Analysis")
    private String class8Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class8RecipeType", dictType = "trial_status")
    private String class8RecipeType;

    // ===== 其他 =====
    @Excel(name = "ui.data.column.cxScheduleResult.totalStock")
    private BigDecimal totalStock;

    @Excel(name = "ui.data.column.cxScheduleResult.lhMachineCode")
    private String lhMachineCode;

    @Excel(name = "ui.data.column.cxScheduleResult.cxRemainQty")
    private BigDecimal cxRemainQty;

    @Excel(name = "ui.data.column.cxScheduleResult.lhRemainQty")
    private BigDecimal lhRemainQty;

    @Excel(name = "ui.data.column.cxScheduleResult.lhClassQty")
    private BigDecimal lhClassQty;
}
