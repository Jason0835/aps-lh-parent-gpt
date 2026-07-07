package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.bill.common.service.IDocService;

/**
 * 斜裁排程结果业务接口。
 */
public interface ICd15ScheduleResultService extends IDocService<Cd15ScheduleResult> {

    /** 自动排程入口 */
    AjaxResult autoSchedule(Cd15ScheduleResult scheduleResult);

    /** 查询自动排程任务 */
    AjaxResult getAutoScheduleTask(String taskId);

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

    /** 发布入口，首期仅保留链路 */
    AjaxResult publish(Cd15ScheduleResult dto, String ids);
}
