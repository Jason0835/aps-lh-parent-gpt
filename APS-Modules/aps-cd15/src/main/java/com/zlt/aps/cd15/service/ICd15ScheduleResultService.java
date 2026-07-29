package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15RollingCheckRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 斜裁排程结果业务接口。
 */
public interface ICd15ScheduleResultService extends IDocService<Cd15ScheduleResult> {

    /** 删除排程结果并压缩同机台 CLASS1 后续生产顺位。 */
    AjaxResult removeScheduleResults(List<Long> ids);

    /** 自动排程入口 */
    AjaxResult autoSchedule(Cd15ScheduleResult scheduleResult);

    /** 查询自动排程任务 */
    AjaxResult getAutoScheduleTask(String taskId);

    /** 查询排程日期对应的启用班次窗口 */
    AjaxResult shiftDates(Cd15InsertOrderRequest request);

    /** 插单预校验 */
    AjaxResult validateInsert(Cd15InsertOrderRequest request);

    /** 插单入口 */
    AjaxResult insert(Cd15InsertOrderRequest request);

    /** 查询插单任务 */
    AjaxResult getInsertTask(String taskId);

    /** 转机台预校验 */
    AjaxResult validateTransferMachine(Cd15TransferMachineRequest request);

    /** 转机台入口 */
    AjaxResult transferMachine(Cd15TransferMachineRequest request);

    /** 查询转机台任务 */
    AjaxResult getTransferMachineTask(String taskId);

    /** 调量预校验 */
    AjaxResult validateChangeQty(Cd15ChangeQtyRequest request);

    /** 调量入口 */
    AjaxResult changeQty(Cd15ChangeQtyRequest request);

    /** 查询调量任务 */
    AjaxResult getChangeQtyTask(String taskId);

    /** 定时滚动检查入口。 */
    AjaxResult checkTimedRolling(Cd15RollingCheckRequest request);

    /** 查询定时滚动任务。 */
    AjaxResult getTimedRollingTask(String taskId);

    /** 按工厂和排程日期查询排程结果。 */
    List<Cd15ScheduleResult> selectByDateAndFactory(Date scheduleDate, String factoryCode);

    /** 独立短事务批量更新发布状态。 */
    int batchUpdateReleaseStatus(List<Cd15ScheduleResult> list, String targetStatus);
}
