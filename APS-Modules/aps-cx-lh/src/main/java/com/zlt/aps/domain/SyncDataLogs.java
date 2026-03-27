package com.zlt.aps.domain;

import lombok.Data;

/**
 * @Description 同步接口日志
 * @Author zlt
 * @Date 2022-3-9 10:27:31
 */
@Data
public class SyncDataLogs {
    /* 数据版本 */
    private String dataVersion;

    /* 请求参数 */
    private String params;

    /* 状态值，1：待处理，2：已反馈，3：处理异常，4，超时失败，6：已完成 */
    private String status;

    /* 反馈信息 */
    private String msg;
}
