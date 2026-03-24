package com.zlt.aps.cxlh.cx.api.domain.vo;

import com.zlt.aps.cxlh.cx.api.domain.vo.CxScheduleTask;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 机台分配模型
 * @author 排产系统
 * @date 2026-03-23
 */
@Data
public class MachineAllocateModel {
    /** 机台编码 */
    private String machineCode;
    /** 机台名称 */
    private String machineName;
    /** 当前负荷（已分配数量） */
    private Integer currentLoad;
    /** 总产能 */
    private Integer totalCapacity;
    /** 剩余产能 */
    private Integer surplusCapacity;
    /** 已分配的胎胚种类数 */
    private Integer embryoTypeCount;
    /** 已分配的胎胚代码列表 */
    private List<String> embryoCodeList = new ArrayList<>();
    /** 已分配的任务列表 */
    private List<CxScheduleTask> taskList = new ArrayList<>();
    /** 是否有历史分配记录 */
    private Boolean hasHistory = false;

    /**
     * 计算剩余产能
     */
    public void calculateSurplusCapacity() {
        this.surplusCapacity = this.totalCapacity - this.currentLoad;
    }
}