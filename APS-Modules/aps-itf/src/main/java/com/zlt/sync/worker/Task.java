package com.zlt.sync.worker;

import com.zlt.sync.domain.AuxReqSyncDataLogs;
import lombok.Data;

import java.util.List;

@Data
public class Task {

    /**
     * 线程id
     */
    private String threadId;

    /**
     * 任务类型
     */
    private String taskType; //SYNC_DATA

    /**
     * 同步数据列表
     */
    private List<AuxReqSyncDataLogs> syncList;

    /**
     * 涉及对接系统
     */
    private List<String> dockSys;

    /**
     * 查询的接口
     */
    private List<String> syncKeys;

}
