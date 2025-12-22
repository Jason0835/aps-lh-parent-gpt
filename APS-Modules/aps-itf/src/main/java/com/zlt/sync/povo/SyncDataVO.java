package com.zlt.sync.povo;

import lombok.Data;

/**
 * 同步接口标志信息VO
 * 配置文件: sync-data-${spring.profiles.active}.yml
 * 例:
 * syncdata:
 *   - {syncName: 当期客户月计划, syncKey: 'CURRENT_CUSTOMER_PLAN', dockSys: CRM0, dataSys:'MPS', taskType: 1, taskId: 1}
 *   - {syncName: 当期客户月计划, syncKey: 'CURRENT_CUSTOMER_PLAN', dockSys: CRM0, dataSys:'APS', taskType: 1, taskId: 1, backIssue:1}
 *
 */
@Data
public class SyncDataVO {
    /**
     * 同步数据块名称
     */
    private String syncName;

    /**
     * 同步数据key
     */
    private String syncKey;
    /**
     * 对接系统
     */
    private String dockSys;

    /**
     * 需求系统
     */
    private String dataSys;
    /**
     * 任务类型 0job, 1trans
     */
    private String taskType;
    /**
     * 任务id
     */
    private String taskId;

    /**
     * 是否回传标志
     */
    private Integer backIssue;

    // 新增
    /**
     * status=0,3超过未返回, 重新发送
     * 单位小时
     */
    private Integer timeout;

    /**
     * 是否重新发送 (默认两小时)
     * 1，需要重发送
     */
    private Integer reSend;

    /**
     * 是否发送MQ
     */
    private Integer noMq;
}
