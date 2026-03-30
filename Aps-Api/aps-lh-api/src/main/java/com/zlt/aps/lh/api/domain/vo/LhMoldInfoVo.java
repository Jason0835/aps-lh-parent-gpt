package com.zlt.aps.lh.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 模具信息
 */
@Data
public class LhMoldInfoVo {

    /**
     * 模具号
     */
    private String moldNo;

    /**
     * 共用数
     */
    private int shareNum;

    /**
     * 使用的规格代号，用于撤回
     */
    private String usedSpecCode;
}
