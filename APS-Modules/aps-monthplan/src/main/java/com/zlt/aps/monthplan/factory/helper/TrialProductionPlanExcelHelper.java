package com.zlt.aps.monthplan.factory.helper;

import com.zlt.aps.monthplan.api.domain.dto.TrialProductionPlanDto;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 试制量试excel导入辅助类
 *
 * @author ZLT
 * @date 20250925
 */
@Slf4j
public class TrialProductionPlanExcelHelper {
    /**
     * excel试制量试数据
     */
    private TrialProductionPlanDto excelRowData;
    /**
     * 转化后的数据
     */
    private MonthPlanProductionFinalResult finalData;

    /**
     * 构建数据
     *
     * @param excelRowData
     */
    public TrialProductionPlanExcelHelper(TrialProductionPlanDto excelRowData) {
        this.excelRowData = excelRowData;
        this.finalData = null;
    }

    public TrialProductionPlanDto getExcelRowData() {
        return excelRowData;
    }

    public MonthPlanProductionFinalResult getFinalData() {
        return finalData;
    }

    public void setFinalData(MonthPlanProductionFinalResult finalData) {
        this.finalData = finalData;
    }
}
