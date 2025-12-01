package com.zlt.aps.factory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanMouldingDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionDayResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分厂月度计划日排产-SKU接口服务
 * 性能优化需要
 *
 * @author ZLT
 * 20250515
 */
public interface IFactoryMonthPlanProductionDayResultService extends IService<MonthPlanProductionDayResult> {

    /**
     * 批量导入分厂月度计划日排产结果
     * @param monthPlanMouldingDayResult 年月、需求计划版本、生产计划版本
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult insertFormImportProductionDay(MonthPlanMouldingDayResult monthPlanMouldingDayResult);
}
