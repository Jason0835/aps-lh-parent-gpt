package com.zlt.aps.monthplan.demand.service;

import lombok.Data;

import java.util.Set;

@Data
public class MonthPlanSaleOrderHelper {
    /**
     * 不存在的客户编码列表
     */
    private Set<String> customCodeSet;
    /**
     * 不存在的物料编码列表
     */
    private Set<String> productCodeSet;
    /**
     * 存在的客户编码列表
     */
    private Set<String> existCustomCodeSet;
    /**
     * 存在的物料编码列表
     */
    private Set<String> existProductCodeSet;
    /**
     * 提示信息
     */
    private String noExistProductInfo;
    /**
     * 提示信息
     */
    private String noExistCustomInfo;
    /**
     * 日志ID
     */
    private Long importLogId;
    /**
     * excel行号
     */
    private Integer rowIndex;
}
