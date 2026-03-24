package com.zlt.aps.cxlh.cx.api.domain.vo;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 成型排产主响应结果
 * @author 排产系统
 * @date 2026-03-23
 */
@Data
public class CxScheduleMainResponse {
    /** 是否成功 */
    private Boolean success = true;
    /** 响应消息 */
    private String msg = "操作成功";
    /** 排产批次号 */
    private String batchNo;
    /** 所有任务列表 */
    private List<CxScheduleTask> taskList = new ArrayList<>();
    /** 续作任务列表 */
    private List<CxScheduleTask> continueTaskList = new ArrayList<>();
    /** 新增任务列表 */
    private List<CxScheduleTask> newTaskList = new ArrayList<>();
    /** 收尾任务列表 */
    private List<CxScheduleTask> endingTaskList = new ArrayList<>();
    /** 试制任务列表 */
    private List<CxScheduleTask> trialTaskList = new ArrayList<>();
    /** 机台列表 */
    private List<MachineAllocateModel> machineList = new ArrayList<>();
    /** 失败任务列表 */
    private List<CxScheduleTask> failTaskList = new ArrayList<>();
}