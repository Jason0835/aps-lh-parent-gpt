package com.zlt.aps.monthplan.factory.helper;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanProductionFinalResultVo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Chen
 * @since 2025/11/4
 */
@Data
@Slf4j
public class ProductionPlanExcelConvertHelper {

    private MonthPlanProductionFinalResultVo excelData;

    private MonthPlanProductionFinalResult resultData;

    public ProductionPlanExcelConvertHelper(MonthPlanProductionFinalResultVo excelData) {
        this.excelData = excelData;
        this.resultData = null;
    }
}
