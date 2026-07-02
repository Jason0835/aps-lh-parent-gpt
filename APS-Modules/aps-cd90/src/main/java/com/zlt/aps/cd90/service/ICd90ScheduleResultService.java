package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

public interface ICd90ScheduleResultService extends IDocService<Cd90ScheduleResult> {

    /**
     * 执行直裁自动排程。
     *
     * @param scheduleResult 自动排程条件，当前使用工厂编码和排程日期
     * @return 自动排程结果
     */
    AjaxResult autoSchedule(Cd90ScheduleResult scheduleResult);

    /** 查询插单弹窗使用的启用班次日期。 */
    AjaxResult shiftDates(Cd90InsertOrderRequest request);

    /** 校验插单请求及锁定顺位。 */
    AjaxResult validateInsert(Cd90InsertOrderRequest request);

    /** 创建插单异步任务。 */
    AjaxResult insertOrder(Cd90InsertOrderRequest request);

    /** 查询插单异步任务。 */
    AjaxResult getInsertTask(String taskId);

    /**
     * 按排程日期 + 工厂编码查询未删除的排程结果。
     *
     * @param scheduleDate 排程日期
     * @param factoryCode 工厂编码
     * @return 排程结果列表
     */
    List<Cd90ScheduleResult> selectByDateAndFactory(Date scheduleDate, String factoryCode);

    /**
     * 按 id 列表批量查询排程结果（含班次字段）。
     *
     * @param ids id 列表
     * @return 排程结果列表
     */
    List<Cd90ScheduleResult> getCd90ScheduleResultListByIds(List<Long> ids);

    /**
     * 批量更新发布状态。独立短事务（REQUIRES_NEW），不受外层 MES 调用异常影响。
     *
     * <p>成功场景：置 IS_RELEASE="1"、publishSuccessCount 累加、newestPublishTime 刷新为当前时间。
     * 失败场景：仅置 isRelease 为传入的目标状态，不累加计数。</p>
     *
     * @param list 待更新的排程结果
     * @param targetStatus 目标状态，取值见 ApsConstant（IS_RELEASE / FAILURE_RELEASE / RELEASING 等）
     * @return 受影响行数
     */
    int batchUpdateReleaseStatus(List<Cd90ScheduleResult> list, String targetStatus);
}
