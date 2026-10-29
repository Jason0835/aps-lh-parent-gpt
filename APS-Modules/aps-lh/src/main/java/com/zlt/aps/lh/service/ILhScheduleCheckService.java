package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.vo.LhScheduleCheckReportVO;

import java.util.Date;

/**
 * 硫化排程结果合理性校验服务。
 *
 * <p>校验键为「工厂编号 + 排程日期」；每次执行先物理删除旧记录再全量重跑落库，保证幂等。</p>
 *
 * @author APS
 * @date 2026/10/27
 */
public interface ILhScheduleCheckService {

    /**
     * 执行校验并落库（幂等替换），返回最新报告。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期（与排程结果行落库的 SCHEDULE_DATE 一致）
     * @param triggerType 触发类型（1-排程 2-导入 3-手动）
     * @return 校验报告
     */
    LhScheduleCheckReportVO execute(String factoryCode, Date scheduleDate, String triggerType);

    /**
     * 查询校验报告（不重新校验）。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期（与排程结果行落库的 SCHEDULE_DATE 一致）
     * @return 校验报告
     */
    LhScheduleCheckReportVO getReport(String factoryCode, Date scheduleDate);

    /**
     * 判断是否存在未确认异常。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期（与排程结果行落库的 SCHEDULE_DATE 一致）
     * @return true-存在未确认异常
     */
    boolean hasPending(String factoryCode, Date scheduleDate);

    /**
     * 将某工厂某业务日的所有异常整体标记为已了解。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期（与排程结果行落库的 SCHEDULE_DATE 一致）
     */
    void acknowledge(String factoryCode, Date scheduleDate);
}
