package com.zlt.aps.monthplan.factory.service;

import com.zlt.aps.monthplan.api.domain.entity.MdmProductInfo;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * 月计划调整通知单导入操作辅助类
 *
 * @author ZLT
 * @date 20250613
 */
@Data
public class MonthPlanAdjustNoticeOrderHelper {
    /**
     * 不存在的物料编码列表
     */
    private Set<String> productCodeSet;
    /**
     * 存在的物料编码列表
     */
    private Map<String, MdmProductInfo> existProductCodeMap;
    /**
     * 提示信息
     */
    private String noExistProductInfo;
    /**
     * 日志ID
     */
    private Long importLogId;
    /**
     * excel行号
     */
    private Integer rowIndex;
}
