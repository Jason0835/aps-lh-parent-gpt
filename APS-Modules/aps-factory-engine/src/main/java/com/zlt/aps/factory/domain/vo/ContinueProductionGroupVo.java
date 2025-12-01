package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 续作排产分组信息对象
 *
 * @author ZLT
 * @date 20250715
 */
@Data
public class ContinueProductionGroupVo implements Serializable {
    /**
     * 续作排产分组值
     */
    private String continueProductionGroupValue;
    /**
     * 实际排产分组组
     */
    private String productionGroupValue;
    /**
     * 本身分组的模台数
     */
    private Integer mouldNumber;
    /**
     * 续作是否拼模排产
     */
    private boolean assemble;
    /**
     * 续作排产信息
     */
    private List<MouldProductionProductVo> continueProductInfoList;
}
