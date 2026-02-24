package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import lombok.Data;

/**
 * 计划调整--辅助类
 *
 * @author ZLT
 * @date 20250528
 */
@Data
public class FactoryMonthFinalPlanHelperVo extends FactoryMonthPlanProdFinal {

    /**
     * 库位排序值
     */
    private Integer sortValue;
}
