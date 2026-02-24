package com.zlt.aps.monthplan.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author Chen
 * @since 2025/9/28
 */
@ApiModel(value = "SAP与施工对照对象", description = "SAP与施工对照对象")
@Data
@Slf4j
public class MdmProductConstructionImportVo extends BaseEntity {

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true)
//    @Excel(name = "ui.data.column.mdmProductConstruction.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode = "116";

    /**
     * 物料编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmProductConstruction.productCode")
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 规格代号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmProductConstruction.importVo.specCode")
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 胎胚号
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.importVo.embryoCode")
    @ApiModelProperty(value = "胎胚号", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 生产版本
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.productionVersion")
    @ApiModelProperty(value = "生产版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * BOM版本
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.bomVersion")
    @ApiModelProperty(value = "BOM版本", name = "bomVersion")
    @TableField(value = "BOM_VERSION")
    private String bomVersion;

    /**
     * 合模压力
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.importVo.mouldClampingPressure")
    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    @TableField(value = "MOULD_CLAMPING_PRESSURE")
    @ImportExcelValidated(required = true)
    private BigDecimal mouldClampingPressure;

    /**
     * 夏季硫化时间文本解析
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.importVo.summerCuringTime")
    @ApiModelProperty(value = "夏季硫化时间文本解析", name = "curingTimeSummerStr")
    private String curingTimeSummerStr;

    /**
     * 冬季硫化时间文本解析
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.importVo.winterCuringTime")
    @ApiModelProperty(value = "冬季硫化时间文本解析", name = "curingTimeWinterStr")
    private String curingTimeWinterStr;

    /**
     * 夏季计划硫化时间文本解析
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.importVo.plan.summerCuringTime")
    @ApiModelProperty(value = "夏季计划硫化时间文本解析", name = "curingTimeSummerPlanStr")
    private String curingTimeSummerPlanStr;

    /**
     * 冬季计划硫化时间文本解析
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.importVo.plan.winterCuringTime")
    @ApiModelProperty(value = "冬季计划硫化时间文本解析", name = "curingTimeWinterPlanStr")
    private String curingTimeWinterPlanStr;

    /**
     * 夏季机械硫化时间--单位秒
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.summerCuringTime")
    @ApiModelProperty(value = "夏季机械硫化时间", name = "curingTime")
    @TableField(value = "CURING_TIME")
    @NotNull(message = "夏季机械硫化时间")
    @ImportExcelValidated(required = true)
    private Integer curingTime;

    /**
     * 冬季机械硫化时间--单位秒
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.winterCuringTime")
    @ApiModelProperty(value = "冬季机械硫化时间", name = "curingTime2")
    @TableField(value = "CURING_TIME2")
    @NotNull(message = "冬季机械硫化时间不能空")
    @ImportExcelValidated(required = true)
    private Integer curingTime2;

    /**
     * 夏季液压硫化时间--单位秒
     */
//    @Excel(name = "ui.data.column.mdmProductConstruction.summerHydraulicPressureCuringTime")
    @ApiModelProperty(value = "夏季液压硫化时间", name = "hydraulicPressureCuringTime")
    @TableField(value = "HYDRAULIC_PRESSURE_CURING_TIME")
    @NotNull(message = "夏季液压硫化时间")
    @ImportExcelValidated(required = true)
    private Integer hydraulicPressureCuringTime;

    /**
     * 冬季液压硫化时间--单位秒
     */
//    @Excel(name = "ui.data.column.mdmProductConstruction.winterHydraulicPressureCuringTime")
    @ApiModelProperty(value = "冬季液压硫化时间", name = "hydraulicPressureCuringTime2")
    @TableField(value = "HYDRAULIC_PRESSURE_CURING_TIME2")
    @NotNull(message = "冬季液压硫化时间")
    @ImportExcelValidated(required = true)
    private Integer hydraulicPressureCuringTime2;

    /**
     * 模具型腔
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.importVo.moldCavity")
    @ApiModelProperty(value = "模具型腔", name = "moldCavity")
    @TableField(value = "MOLD_CAVITY")
    private String moldCavity;

    /**
     * 根据硫化时间文本解析赋值对应的硫化时间
     * 解析方法参考BUG：17893
     */
    public void setCuringTimeByTimeStr() {
        if (StringUtils.isNotBlank(this.curingTimeSummerStr)) {
            int time = getCuringTime(this.curingTimeSummerStr);
            this.curingTime = time;
            this.hydraulicPressureCuringTime = time;
        } else if (StringUtils.isNotBlank(this.curingTimeSummerPlanStr)) {
            int time = getCuringTime(this.curingTimeSummerPlanStr);
            this.curingTime = time;
            this.hydraulicPressureCuringTime = time;
        }
        if (StringUtils.isNotBlank(this.curingTimeWinterStr)) {
            int time = getCuringTime(this.curingTimeWinterStr);
            this.curingTime2 = time;
            this.hydraulicPressureCuringTime2 = time;
        } else if (StringUtils.isNotBlank(this.curingTimeWinterPlanStr)) {
            int time = getCuringTime(this.curingTimeWinterPlanStr);
            this.curingTime2 = time;
            this.hydraulicPressureCuringTime2 = time;
        }
    }

    private int getCuringTime(String curingTimeWinterStr) {
        String model = curingTimeWinterStr.substring(0, 1);
        int time = 0;
        try {
            time = Integer.parseInt(curingTimeWinterStr.substring(1));
        } catch (NumberFormatException e) {
            log.error("解析时间失败：{}", curingTimeWinterStr);
        }
        int addTime = 0;
        if ("H".equals(model)) {
            addTime = 30;
        }
        return 60 * time + addTime + 15;
    }
}
