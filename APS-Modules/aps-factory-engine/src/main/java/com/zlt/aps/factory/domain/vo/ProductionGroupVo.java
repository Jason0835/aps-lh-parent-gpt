package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 获取排产分组基础信息
 *
 * @author ZLT
 * 20250715
 */
@Data
public class ProductionGroupVo implements Serializable {
    /**
     * 模台数 1 单模台 2 双模台
     */
    private Integer mouldNumber;
    /**
     * 个数
     */
    private Integer groupCount;
}
