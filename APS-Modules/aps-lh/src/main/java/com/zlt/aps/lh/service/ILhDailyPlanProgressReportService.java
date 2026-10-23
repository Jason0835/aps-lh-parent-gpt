package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.dto.LhDailyPlanProgressReportDTO;

/**
 * 日排产进度报表服务。
 *
 * <p>以硫化排程页面的排程日期为基准，输出「基准 + 增机 + 延迟」SKU 的日排产进度 Excel：
 * 固定列取月计划定稿，日期列取定稿每日排产量，硫化机数量与 T+2日排产信息按排程口径计算。</p>
 *
 * @author APS
 */
public interface ILhDailyPlanProgressReportService {

    /**
     * 导出日排产进度报表 Excel。
     *
     * @param queryDTO 查询条件（工厂编码 + 排程日期）
     * @return Excel 文件字节数组
     */
    byte[] exportDailyPlanProgressReport(LhDailyPlanProgressReportDTO queryDTO);
}
