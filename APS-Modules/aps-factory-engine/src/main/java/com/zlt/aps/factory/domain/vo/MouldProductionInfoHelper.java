package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 双模排产-衔接分组辅助类
 *
 * @author ZLT
 * @date 20250807
 */
@Data
public class MouldProductionInfoHelper implements Serializable {
    /**
     * 第一副模具最后排产信息
     */
    private ProductionInfoVo finalFirstProductionInfo;
    /**
     * 第二副模具最后排产信息
     */
    private ProductionInfoVo finalSecondProductionInfo;
}
