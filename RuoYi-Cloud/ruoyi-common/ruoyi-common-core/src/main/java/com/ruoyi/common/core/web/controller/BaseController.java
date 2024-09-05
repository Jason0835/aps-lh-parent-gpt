package com.ruoyi.common.core.web.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.sql.SqlUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * web层通用数据处理
 * 
 * @author ruoyi
 */
public class BaseController
{
    protected final Logger logger = LoggerFactory.getLogger(BaseController.class);

    @Value("${fileEncrypt.ll.enabled:false}")
    public Boolean useFileEncrypt;

    private static Pattern humpPattern = Pattern.compile("[A-Z]");

    /**
     * 将前台传递过来的日期格式的字符串，自动转化为Date类型
     */
    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        // Date 类型转换
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport()
        {
            @Override
            public void setAsText(String text)
            {
                setValue(DateUtils.parseDate(text));
            }
        });
    }

    /**
     * 设置请求分页数据
     */
    protected void startPage()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        if (StringUtils.isNotNull(pageNum) && StringUtils.isNotNull(pageSize))
        {
            PageHelper.startPage(pageNum, pageSize);
        }
    }


    /**
     *
     * @param orderby 排序字段，例如：CREATE_TIME desc, GLUE_GROUP_CODE desc
     */
    protected void startPage(String orderby)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        pageNum = (pageNum == null ? 1 : pageNum);
        pageSize = (pageSize == null ? 10000000 : pageSize);
        String orderBy = SqlUtil.escapeOrderBySql(orderby);
        PageHelper.startPage(pageNum, pageSize, orderBy);
    }

    /**
     * 响应请求分页数据
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected TableDataInfo getDataTable(List<?> list)
    {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setRows(list);
        rspData.setMsg(I18nUtil.getMessage("common.msg.base.query.success"));
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }

    /**
     * 响应返回结果
     * 
     * @param rows 影响行数
     * @return 操作结果
     */
    protected AjaxResult toAjax(int rows)
    {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 获得排序拼接字符串（字段名都转成下划线），格式：字段名 + 排序方式
     * @return
     */
    protected String orderStr() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        if(pageDomain == null) {
            return null;
        }
        String orderByColumn = pageDomain.getOrderByColumn();
        if(StringUtils.isBlank(orderByColumn) || "null".equals(orderByColumn)) {
            return null;
        }
        orderByColumn = humpToLine(orderByColumn);  //排序字段，并转成下划线格式
        String isAsc = pageDomain.getIsAsc();  //排序方式
        return orderByColumn + " " + isAsc;
    }

    /**
     * 驼峰转下划线,最后转为大写（首字母是小写的）
     * @param str
     * @return
     */
    private String humpToLine(String str) {
        if(str == null) {
            return null;
        }
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString().toLowerCase();
    }
}
