package com.zlt.aps.lh.engine.domain;

import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import lombok.Data;

/**
 * 模具变动单引擎端实体对象
 */
@Data
public class LhEngineMoldChangePlan extends LhMoldChangePlan {

    /**
     * 前规格品号
     */
    private String beforeSapCode;

    /**
     * 单条胎硫化时长
     */
    private Double lhSingleTireTime;

    /**
     * 成型工单号串，多个进行分号拼接
     */
    private String sourceCxOrder;

    /**
     * 成型批次号
     */
    private String cxBatchNo;

    /**
     * 当前硫化机在成型工序确认的使用模具数
     */
    private Integer useMoldNum;
}
