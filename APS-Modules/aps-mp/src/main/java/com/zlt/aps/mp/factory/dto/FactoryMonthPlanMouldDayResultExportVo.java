package com.zlt.aps.mp.factory.dto;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class FactoryMonthPlanMouldDayResultExportVo extends FactoryMonthPlanMouldDayResult {
    private static final long serialVersionUID = 1L;
    
    /**
     * 导出数据类型，1：明细记录，2：胎胚种类数，3：小计、4：总计
     */
    @ApiModelProperty(value = "导出数据类型", name = "dataType")
    private String dataType;
    
    /**
     * 中优先级净需求
     */
    @ApiModelProperty(value = "中优先级净需求", name = "midQty")
    private Integer midQty;

    /**
     * 周期储备需求
     */
    @ApiModelProperty(value = "周期储备需求", name = "cycleReserveQty")
    private Integer cycleReserveQty;

}
