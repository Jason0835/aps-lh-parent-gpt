package com.zlt.aps.mps.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 接口同步日志表
 * @TableName T_SERVICE_SYNC_LOG
 */
@Data
public class TServiceSyncLog extends ApsBaseEntity {
    /**
     * 主键ID，对应序列SEQ_SERVICE_SYNC_LOG
     */
    private Long id;

    /**
     * 接口类型ServiceTypeEnum
     */
    private String serviceType;

    /**
     * 最终的处理状态。0：成功；1：失败
     */
    private String serviceStatus;

    /**
     * 记录接口相关请求的参数，格式为JSON
     */
    private String serviceParams;

    /**
     * 保存接口执行反馈的结果(JAR包中反馈的结果存储)
     */
    private String serviceResult;

    private static final long serialVersionUID = 1L;
}