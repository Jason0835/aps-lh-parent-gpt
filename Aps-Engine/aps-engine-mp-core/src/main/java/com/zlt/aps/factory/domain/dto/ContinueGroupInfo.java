package com.zlt.aps.factory.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 排产计划-续作分组信息
 * TBR-在机结构对应机台
 * PCR-在机英寸(寸别、寸口)对应机台
 *
 * @author ZLT
 * @date 20251225
 */
@Data
public class ContinueGroupInfo implements Serializable {

    /**
     * 成型机台编号
     */
    private String cxMachineCode;
    /**
     * 分组信息--TBR = 结构名
     * PCR = 英寸
     */
    private String groupName;
}
