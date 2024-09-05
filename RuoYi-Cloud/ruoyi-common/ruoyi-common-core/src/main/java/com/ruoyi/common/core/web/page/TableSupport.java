package com.ruoyi.common.core.web.page;

import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;

/**
 * 表格数据处理
 *
 * @author ruoyi
 */
public class TableSupport {
    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum" ;

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize" ;

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn" ;

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc" ;

    /**
     * 封装分页对象
     */
    public static PageDomain getPageDomain() {
        PageDomain pageDomain = new PageDomain();
        String value = StringUtils.getIfEmpty(ServletUtils.getParameter(PAGE_NUM), () ->
                ServletUtils.getHeader(PAGE_NUM));
        pageDomain.setPageNum(Convert.toInt(value));

        value = StringUtils.getIfEmpty(ServletUtils.getParameter(PAGE_SIZE), () ->
                ServletUtils.getHeader(PAGE_SIZE));
        pageDomain.setPageSize(Convert.toInt(value));

        value = StringUtils.getIfEmpty(ServletUtils.getParameter(ORDER_BY_COLUMN), () ->
                ServletUtils.getHeader(ORDER_BY_COLUMN));
        pageDomain.setOrderByColumn(String.valueOf(value));

        value = StringUtils.getIfEmpty(ServletUtils.getParameter(IS_ASC), () ->
                ServletUtils.getHeader(IS_ASC));
        pageDomain.setIsAsc(String.valueOf(value));

        return pageDomain;
    }

    public static PageDomain buildPageRequest() {
        return getPageDomain();
    }
}
