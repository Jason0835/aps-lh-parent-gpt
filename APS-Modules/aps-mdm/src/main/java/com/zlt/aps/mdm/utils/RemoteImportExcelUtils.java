package com.zlt.aps.mdm.utils;

import com.alibaba.fastjson.JSONArray;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.mdm.wrapper.AsyncHttpServletRequestWrapper;
import com.zlt.aps.mdm.api.service.IRemoteImportErrorLogService;
import com.zlt.aps.mdm.api.service.IRemoteImportLogService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 不经过网关，异步线程操作
 */
public class RemoteImportExcelUtils {

    /**
     * 更新日志结果数
     */
    public static void updateImportLogAndFormatMsg(ImportLog importLog, AjaxResult ajaxResult, IRemoteImportLogService iImportLogService) {
        if (null == ajaxResult) {
            throw new CustomException("接口数据返回空，后台服务没有开启");
        } else {
            Object msg = ajaxResult.get("msg");
            if (null == msg) {
                throw new CustomException("接口数据返回空，后台服务没有开启");
            } else {
                String[] message = ajaxResult.get("msg").toString().split(",");
                switch (message.length) {
                    case 2:
                        importLog.setSuccessNum(Long.valueOf(message[1]));
                        importLog.setFailNum(0L);
                        ajaxResult.put("msg", StringUtils.format(message[0], new Object[]{message[1]}));
                        break;
                    case 3:
                        importLog.setSuccessNum(Long.valueOf(message[1]));
                        importLog.setFailNum(Long.valueOf(message[2]));
                        ajaxResult.put("msg", StringUtils.format(message[0], new Object[]{message[1], message[2]}));
                }

                iImportLogService.edit(importLog);
            }
        }
    }

    /**
     * 记录日志错误明细
     */
    public static void saveImportErrorLogs(AjaxResult ajaxResult, IRemoteImportErrorLogService iImportErrorLogService) {
        if (ajaxResult.get("code").equals(500)) {
            List<ImportErrorLog> importErrorLogs = (List)StringUtils.cast(ajaxResult.get("data"));
            if (CollectionUtils.isNotEmpty(importErrorLogs)) {
                String listTxt = JSONArray.toJSONString(importErrorLogs);
                List<ImportErrorLog> importErrorLogList = JSONArray.parseArray(listTxt, ImportErrorLog.class);
                iImportErrorLogService.insertImportErrorLogList(importErrorLogList);
            }
        }
    }

    /**
     * 拷贝主线程的header信息
     */
    public static ServletRequestAttributes copyRequestHeaderAttribute() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        AsyncHttpServletRequestWrapper requestWrapper = new AsyncHttpServletRequestWrapper(attributes != null ? attributes.getRequest() : null);
        Map<String, String> headMap = new HashMap<>();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            Enumeration<String> headNameList = request.getHeaderNames();
            while (headNameList.hasMoreElements()) {
                String key = headNameList.nextElement();
                headMap.put(key.toLowerCase(), request.getHeader(key));
            }
        }
        requestWrapper.setHeadMap(headMap);
        ServletRequestAttributes virtualAttr = new ServletRequestAttributes(requestWrapper);
        return virtualAttr;
    }
}
