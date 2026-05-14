package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.vo.ScheduleSummaryReportVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 排产小结报表服务接口
 *
 * <p>提供排产小结报表的导出功能，
 * 聚合成型排程结果和硫化排程结果数据生成报表。</p>
 *
 * @author APS Team
 */
public interface IScheduleSummaryReportService {

    /**
     * 导出排产小结报表
     *
     * @param queryVO 查询条件，包含排程日期和分厂编码
     * @return Excel文件字节数组
     */
    byte[] exportScheduleSummaryReport(ScheduleSummaryReportVO queryVO);

    /**
     * 构建排产小结导出数据（tableMap和dataList），
     * 供外部调用方将排产小结作为子sheet嵌入到多sheet导出流程中。
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编码
     * @return 包含 tableMap（模板占位符映射）和 dataList（列表数据）的Map
     */
    Map<String, Object> buildScheduleSummaryExportData(Date scheduleDate, String factoryCode);
}
