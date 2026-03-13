package com.zlt.aps.mp.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 试制量试计划导入
 *
 * @author ZLT
 * @date 20250924
 */
@Data
public class TrialProductionPlanDto extends BaseEntity {

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.factoryCode", dictType = "biz_factory_name", sort = 1)
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.year", sort = 2)
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.month", sort = 3)
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;
    /**
     * 生产物料编号
     */
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.productDesc", sort = 5)
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    private String productDesc;
    /**
     * 施工阶段
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.schedulingType", dictType = "biz_construction_stage", sort = 6)
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    private Integer constructionStage;
    /**
     * 成型法
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.mouldMethod", dictType = "MACHINE_TYPE", sort = 7)
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    private String mouldMethod;
    /**
     * 规格代号
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.specCode", sort = 8)
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "规格代号", name = "specCode")
    private String specCode;

    /**
     * 生胎代码
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.embryoCode", sort = 9)
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    private String embryoCode;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.specifications", sort = 10)
    @ApiModelProperty(value = "规格", name = "specifications")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.pattern", sort = 11)
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;
    /**
     * 实际生产需求(含损耗)
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.factProdReqQty", cellType = Excel.ColumnType.NUMERIC, sort = 12)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "实际生产需求(含损耗)", name = "factProdReqQty")
    private Long factProdReqQty;

    /**
     * 模具数
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.mouldQty", sort = 14)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "模具数", name = "mouldQty")
    private Integer mouldQty;
    /**
     * 备注说明
     */
    @Excel(name = "ui.common.column.remark", sort = 15)
    @ApiModelProperty("备注")
    private String remark;

    /**
     * DAY_1
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day1", cellType = Excel.ColumnType.NUMERIC, sort = 30)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_1", name = "day1")
    private Long day1;

    /**
     * DAY_2
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day2", cellType = Excel.ColumnType.NUMERIC, sort = 31)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_2", name = "day2")
    private Long day2;

    /**
     * DAY_3
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day3", cellType = Excel.ColumnType.NUMERIC, sort = 32)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_3", name = "day3")
    private Long day3;

    /**
     * DAY_4
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day4", cellType = Excel.ColumnType.NUMERIC, sort = 33)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_4", name = "day4")
    private Long day4;

    /**
     * DAY_5
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day5", cellType = Excel.ColumnType.NUMERIC, sort = 34)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_5", name = "day5")
    private Long day5;

    /**
     * DAY_6
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day6", cellType = Excel.ColumnType.NUMERIC, sort = 35)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_6", name = "day6")
    private Long day6;

    /**
     * DAY_7
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day7", cellType = Excel.ColumnType.NUMERIC, sort = 36)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_7", name = "day7")
    private Long day7;

    /**
     * DAY_8
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day8", cellType = Excel.ColumnType.NUMERIC, sort = 37)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_8", name = "day8")
    private Long day8;

    /**
     * DAY_9
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day9", cellType = Excel.ColumnType.NUMERIC, sort = 38)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_9", name = "day9")
    private Long day9;

    /**
     * DAY_10
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day10", cellType = Excel.ColumnType.NUMERIC, sort = 39)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_10", name = "day10")
    private Long day10;

    /**
     * DAY_11
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day11", cellType = Excel.ColumnType.NUMERIC, sort = 40)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_11", name = "day11")
    private Long day11;

    /**
     * DAY_12
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day12", cellType = Excel.ColumnType.NUMERIC, sort = 41)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_12", name = "day12")
    private Long day12;

    /**
     * DAY_13
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day13", cellType = Excel.ColumnType.NUMERIC, sort = 42)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_13", name = "day13")
    private Long day13;

    /**
     * DAY_14
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day14", cellType = Excel.ColumnType.NUMERIC, sort = 43)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_14", name = "day14")
    private Long day14;

    /**
     * DAY_15
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day15", cellType = Excel.ColumnType.NUMERIC, sort = 44)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_15", name = "day15")
    private Long day15;

    /**
     * DAY_16
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day16", cellType = Excel.ColumnType.NUMERIC, sort = 45)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_16", name = "day16")
    private Long day16;

    /**
     * DAY_17
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day17", cellType = Excel.ColumnType.NUMERIC, sort = 46)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_17", name = "day17")
    private Long day17;

    /**
     * DAY_18
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day18", cellType = Excel.ColumnType.NUMERIC, sort = 47)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_18", name = "day18")
    private Long day18;

    /**
     * DAY_19
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day19", cellType = Excel.ColumnType.NUMERIC, sort = 48)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_19", name = "day19")
    private Long day19;

    /**
     * DAY_20
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day20", cellType = Excel.ColumnType.NUMERIC, sort = 49)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_20", name = "day20")
    private Long day20;

    /**
     * DAY_21
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day21", cellType = Excel.ColumnType.NUMERIC, sort = 50)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_21", name = "day21")
    private Long day21;

    /**
     * DAY_22
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day22", cellType = Excel.ColumnType.NUMERIC, sort = 51)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_22", name = "day22")
    private Long day22;

    /**
     * DAY_23
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day23", cellType = Excel.ColumnType.NUMERIC, sort = 52)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_23", name = "day23")
    private Long day23;

    /**
     * DAY_24
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day24", cellType = Excel.ColumnType.NUMERIC, sort = 53)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_24", name = "day24")
    private Long day24;

    /**
     * DAY_25
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day25", cellType = Excel.ColumnType.NUMERIC, sort = 54)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_25", name = "day25")
    private Long day25;

    /**
     * DAY_26
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day26", cellType = Excel.ColumnType.NUMERIC, sort = 55)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_26", name = "day26")
    private Long day26;

    /**
     * DAY_27
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day27", cellType = Excel.ColumnType.NUMERIC, sort = 56)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_27", name = "day27")
    private Long day27;

    /**
     * DAY_28
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day28", cellType = Excel.ColumnType.NUMERIC, sort = 57)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_28", name = "day28")
    private Long day28;

    /**
     * DAY_29
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day29", cellType = Excel.ColumnType.NUMERIC, sort = 58)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_29", name = "day29")
    private Long day29;

    /**
     * DAY_30
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day30", cellType = Excel.ColumnType.NUMERIC, sort = 59)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_30", name = "day30")
    private Long day30;

    /**
     * DAY_31
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.day31", cellType = Excel.ColumnType.NUMERIC, sort = 60)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_31", name = "day31")
    private Long day31;

    /**
     * 获取分厂同版本计划key
     *
     * @return
     */
    public String getSameProductionVersionKey() {
        String sameProductionVersionKeyFormat = "%s|*|%s|*|%s";
        return String.format(sameProductionVersionKeyFormat, getFactoryCode(), getYear(), getMonth());
    }
}
