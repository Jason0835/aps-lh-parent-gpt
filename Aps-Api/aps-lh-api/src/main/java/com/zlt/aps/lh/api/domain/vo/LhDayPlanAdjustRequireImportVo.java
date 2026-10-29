package com.zlt.aps.lh.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 硫化日计划调整需求导入 VO。
 *
 * <p>本 VO 仅用于导入模板下载与导入解析，与页面列表行转列实体
 * {@code LhDayPlanAdjustRequire} 分离，避免导入字段污染数据库实体。</p>
 *
 * <p>工厂不进入导入字段：导入时工厂统一取页面当前搜索工厂，Excel 中的工厂列不参与解析；
 * 年月保留为导入字段，用于与页面搜索年月做整批一致性校验。</p>
 */
@Data
@ApiModel(value = "硫化日计划调整需求导入对象", description = "硫化日计划调整需求导入对象")
public class LhDayPlanAdjustRequireImportVo extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 年月，格式 yyyy-MM 或 yyyyMM */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.yearMonth", width = 14)
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty("年月，格式 yyyy-MM 或 yyyyMM")
    private String yearMonth;

    /** NC物料编码，仅用于定位调整槽位，不允许通过导入修改 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.materialCode", width = 22)
    @ImportExcelValidated(required = true, maxLength = 32)
    @ApiModelProperty("NC物料编码")
    private String materialCode;

    /** 产品状态，字典 lh_trial_status */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.productStatus", dictType = "lh_trial_status", width = 16)
    @ImportExcelValidated(required = true, maxLength = 20, dictType = "lh_trial_status")
    @ApiModelProperty("产品状态")
    private String productStatus;

    /** 调整1数量，可正可负 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustQty1", width = 14)
    @ImportExcelValidated(number = true)
    @ApiModelProperty("调整1数量")
    private BigDecimal adjustQty1;

    /** 调整1原因 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustReason1", width = 32)
    @ImportExcelValidated(maxLength = 2000)
    @ApiModelProperty("调整1原因")
    private String adjustReason1;

    /** 调整2数量，可正可负 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustQty2", width = 14)
    @ImportExcelValidated(number = true)
    @ApiModelProperty("调整2数量")
    private BigDecimal adjustQty2;

    /** 调整2原因 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustReason2", width = 32)
    @ImportExcelValidated(maxLength = 2000)
    @ApiModelProperty("调整2原因")
    private String adjustReason2;

    /** 调整3数量，可正可负 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustQty3", width = 14)
    @ImportExcelValidated(number = true)
    @ApiModelProperty("调整3数量")
    private BigDecimal adjustQty3;

    /** 调整3原因 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustReason3", width = 32)
    @ImportExcelValidated(maxLength = 2000)
    @ApiModelProperty("调整3原因")
    private String adjustReason3;
}
