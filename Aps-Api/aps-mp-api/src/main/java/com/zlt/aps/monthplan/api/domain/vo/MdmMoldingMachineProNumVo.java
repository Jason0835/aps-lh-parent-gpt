package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成型机信息
 *
 * @author ZLT
 * @date 20250619
 */
@Data
public class MdmMoldingMachineProNumVo extends BaseMoldingMachineCapacityVo {

    private static final long serialVersionUID = 1L;

    /**
     * 成型法:来源于数据字典molding_method
     */
    @ApiModelProperty(value = "成型法:来源于数据字典molding_method", name = "mouldMethod")
    private Integer mouldMethod;

    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;

    /**
     * 数量
     */
    @ApiModelProperty(value = "数量", name = "proNum")
    private Integer proNum;

    /**
     * 平均定额
     */
    @ApiModelProperty(value = "平均定额", name = "averageQuota")
    private BigDecimal averageQuota;

    /**
     * 总额定额
     */
    @ApiModelProperty(value = "平均定额", name = "averageQuota")
    private BigDecimal quota;

    /**
     * 机台总数
     */
    @ApiModelProperty(value = "机台总数", name = "machineSumNum")
    private Integer machineSumNum;


    /**
     * 获取寸口产能分组key
     * 寸口|*|成型法
     *
     * @return
     */
    public String getSizeCapacityGroupKey() {
        String groupKey = "%s|*|%s";
        return String.format(groupKey, getProSize(), getMouldMethod());
    }
}
