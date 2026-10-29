package com.zlt.aps.lh.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 硫化日计划调整需求导出 VO。
 *
 * <p>导出列与导入模板 {@code LhDayPlanAdjustRequireImportVo} 保持完全一致的紧凑列，
 * 保证导出的文件修改后可直接作为导入文件使用；工厂列位于最后一列，仅用于归档核对，
 * 导入时忽略该列，工厂统一以页面当前搜索工厂为准。</p>
 */
@Data
@ApiModel(value = "硫化日计划调整需求导出对象", description = "硫化日计划调整需求导出对象")
public class LhDayPlanAdjustRequireExportVo extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编号，末列仅用于归档核对，导入时忽略该列 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.factoryCode", dictType = "biz_factory_name", width = 18)
    @ApiModelProperty("工厂编号")
    private String factoryCode;

    /** 年月，格式 yyyy-MM */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.yearMonth", width = 14)
    @ApiModelProperty("年月，格式 yyyy-MM")
    private String yearMonth;

    /** NC物料编码 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.materialCode", width = 22)
    @ApiModelProperty("NC物料编码")
    private String materialCode;

    /** 产品状态，字典 lh_trial_status */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.productStatus", dictType = "lh_trial_status", width = 16)
    @ApiModelProperty("产品状态")
    private String productStatus;

    /** 调整1数量 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustQty1", width = 14)
    @ApiModelProperty("调整1数量")
    private BigDecimal adjustQty1;

    /** 调整1原因 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustReason1", width = 32)
    @ApiModelProperty("调整1原因")
    private String adjustReason1;

    /** 调整2数量 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustQty2", width = 14)
    @ApiModelProperty("调整2数量")
    private BigDecimal adjustQty2;

    /** 调整2原因 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustReason2", width = 32)
    @ApiModelProperty("调整2原因")
    private String adjustReason2;

    /** 调整3数量 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustQty3", width = 14)
    @ApiModelProperty("调整3数量")
    private BigDecimal adjustQty3;

    /** 调整3原因 */
    @Excel(name = "ui.data.column.lhDayPlanAdjustRequire.importAdjustReason3", width = 32)
    @ApiModelProperty("调整3原因")
    private String adjustReason3;

}
