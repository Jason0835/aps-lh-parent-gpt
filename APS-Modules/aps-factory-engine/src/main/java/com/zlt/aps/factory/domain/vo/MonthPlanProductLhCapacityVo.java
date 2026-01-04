package com.zlt.aps.factory.domain.vo;

import com.zlt.aps.factory.enums.DayVulcanizationModeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工厂月度排产-SKU日硫化产能对象
 *
 * @author ZLT
 * @date 20251209
 */
@Data
public class MonthPlanProductLhCapacityVo implements Serializable {
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
     * MES的日硫化量
     */
    @ApiModelProperty(value = "MES的日硫化量", name = "mesCapacity")
    private Integer mesCapacity;

    /**
     * 标准日硫化量
     */
    @ApiModelProperty(value = "标准日硫化量", name = "standardCapacity")
    private Integer standardCapacity;

    /**
     * APS的日硫化量
     */
    @ApiModelProperty(value = "APS的日硫化量", name = "apsCapacity")
    private Integer apsCapacity;

    /**
     * 总硫化时间(单位s)
     */
    @ApiModelProperty(value = "总硫化时间(单位s)", name = "vulcanizationTime")
    private BigDecimal vulcanizationTime;

    /**
     * 类型 01 模具关系 02 新模具到货计划
     */
    @ApiModelProperty(value = "类型", name = "type")
    private String type;
    /**
     * 日硫化量
     */
    @ApiModelProperty(value = "总硫化时间(单位s)", name = "vulcanizationTime")
    private Integer dayVulcanizationQty;

    /**
     * 日硫化量计算(双模)
     *
     * @param mode
     */
    public void calculateDayVulcanizationQty(DayVulcanizationModeEnum mode) {
        if (DayVulcanizationModeEnum.MES_CAPACITY == mode) {
            dayVulcanizationQty = mesCapacity;
            return;
        }
        if (DayVulcanizationModeEnum.STANDARD_CAPACITY == mode) {
            dayVulcanizationQty = standardCapacity;
            return;
        }
        if (DayVulcanizationModeEnum.APS_CAPACITY == mode) {
            dayVulcanizationQty = apsCapacity;
            return;
        }
        dayVulcanizationQty = standardCapacity;
    }

}
