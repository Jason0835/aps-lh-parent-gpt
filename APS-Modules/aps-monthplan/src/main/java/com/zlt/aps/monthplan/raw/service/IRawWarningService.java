package com.zlt.aps.monthplan.raw.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.RawWarningRecord;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IRawWarningService extends IService<RawWarningRecord> {

    /**
     * 执行用量偏差预警
     */
    AjaxResult executeUsageDeviationWarning(String factoryCode, Integer year, Integer week, Integer month);

    /**
     * 执行新材料预警
     */
    AjaxResult executeNewMaterialWarning(String factoryCode, Integer currentYear, Integer currentMonth);

    /**
     * 同步周维度实际用量数据
     */
    AjaxResult syncWeekActualUsage(String factoryCode, Integer year, Integer week, Integer month);

    /**
     * 查询预警记录
     */
    List<RawWarningRecord> queryWarningRecords(String factoryCode, String warningType,
                                               Date startDate, Date endDate, String status);

    /**
     * 处理预警记录
     */
    AjaxResult handleWarning(Long id, String handler, String opinion);

    /**
     * 获取预警统计
     */
    Map<String, Object> getWarningStatistics(String factoryCode, String warningType, Integer days);
}