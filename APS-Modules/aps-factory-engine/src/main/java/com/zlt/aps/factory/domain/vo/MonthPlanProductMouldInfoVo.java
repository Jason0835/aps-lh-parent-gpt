package com.zlt.aps.factory.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工厂月度排产物料模具信息
 *
 * @author ZLT
 * @date 20251209
 */
@Data
public class MonthPlanProductMouldInfoVo implements Serializable {
    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    private String materialDesc;

    /**
     * 型腔模号
     */
    @ApiModelProperty(value = "型腔模号", name = "mouldCode")
    private String mouldCode;

    /**
     * 主花纹
     */
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    private String mainPattern;

    /**
     * 型腔模号-模具台账
     */
    @ApiModelProperty(value = "型腔模号-模具台账", name = "baseMouldCode")
    private String baseMouldCode;

    /**
     * 模具类型
     */
    @ApiModelProperty(value = "模具类型", name = "mouldType")
    private String mouldType;

    /**
     * 模具状态
     */
    @ApiModelProperty(value = "模具状态", name = "mouldStatus")
    private String mouldStatus;

    /**
     * 模壳标准
     */
    @ApiModelProperty(value = "模壳标准", name = "shellStandard")
    private String shellStandard;

    /**
     * 物流状态
     */
    @ApiModelProperty(value = "物流状态", name = "logisticsStatus")
    private String logisticsStatus;

    /**
     * 关系类型 01 sku与模具关系 02 新模具到货计划
     */
    @ApiModelProperty(value = "关系类型", name = "relationType")
    private String relationType;

    /**
     * 结构名，与计划匹配后补充
     */
    private String structureName;
    /**
     * 上机日期--默认为空
     */
    private Date boardingDate;

    /**
     * 结构|*|主花纹
     *
     * @return
     */
    public String getStructureNameAndMainPattern() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, structureName, mainPattern);
    }
}
