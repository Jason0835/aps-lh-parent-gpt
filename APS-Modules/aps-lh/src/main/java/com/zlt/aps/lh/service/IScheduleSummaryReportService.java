package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.vo.ScheduleSummaryReportVO;

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
}
