package com.zlt.aps.monthplan.factory.helper;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chen
 * @since 2025/11/4
 */
@Slf4j
@Data
public class ProductionPlanExcelImportHelper {

    /**
     * 无排产号的新增规格
     */
    private List<MonthPlanProductionFinalResult> insertList = new ArrayList<>();

    /**
     * 有排产单号的更新列表
     */
    private List<MonthPlanProductionFinalResult> updateList = new ArrayList<>();

    /**
     * 解析后的数据是否为空
     * @return 结果
     */
    public Boolean isImportDataEmpty() {
        return insertList.isEmpty() && updateList.isEmpty();
    }
}
