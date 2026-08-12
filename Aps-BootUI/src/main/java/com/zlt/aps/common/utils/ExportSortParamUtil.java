package com.zlt.aps.common.utils;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 基础资料列表排序参数传递工具类。
 *
 * <p>BootUI 接收页面排序参数后，将其写入 Feign 实体的 {@code params}，
 * 使微服务列表与导出使用同一排序口径。</p>
 *
 * @author zlt
 */
public final class ExportSortParamUtil {

    private static final String ORDER_BY_FIELD = "orderBy";
    private static final String ORDER_BY_COLUMN_FIELD = "orderByColumn";
    private static final String ORDER_ASC_FIELD = "isAsc";
    private static final String ASC_VALUE = "1";
    private static final String DESC_VALUE = "0";

    private ExportSortParamUtil() {
    }

    /**
     * 将当前 HTTP 请求中的排序参数写入业务实体。
     *
     * @param entity 需通过 Feign 传递的查询实体
     * @param request 当前 HTTP 请求
     */
    public static void applySortParams(BaseEntity entity, HttpServletRequest request) {
        if (request == null) {
            return;
        }
        applySortParams(entity, request.getParameter(ORDER_BY_COLUMN_FIELD), request.getParameter(ORDER_ASC_FIELD));
    }

    /**
     * 将页面排序参数规范化后写入业务实体。
     *
     * @param entity 查询或导出实体
     * @param orderByColumn 页面字段名称
     * @param isAsc 页面排序方向，支持 asc、desc、1、0
     */
    public static void applySortParams(BaseEntity entity, String orderByColumn, String isAsc) {
        if (entity == null || StringUtils.isBlank(orderByColumn)) {
            return;
        }
        Map<String, Object> params = entity.getParams();
        if (params == null) {
            params = new HashMap<>();
            entity.setParams(params);
        }
        params.put(ORDER_BY_FIELD, orderByColumn);
        params.put(ORDER_ASC_FIELD, isAscending(isAsc) ? ASC_VALUE : DESC_VALUE);
    }

    /**
     * 判断页面传入的排序方向是否为升序。
     *
     * @param isAsc 页面排序方向
     * @return 升序返回 true，否则返回 false
     */
    private static boolean isAscending(String isAsc) {
        return ASC_VALUE.equals(isAsc) || "asc".equalsIgnoreCase(isAsc);
    }
}
