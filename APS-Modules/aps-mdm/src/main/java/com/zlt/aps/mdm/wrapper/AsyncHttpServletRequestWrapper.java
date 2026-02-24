package com.zlt.aps.mdm.wrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Map;

/**
 * 异步线程保留header的数据
 */
public class AsyncHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private Map<String, String> headMap;

    public AsyncHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public void setHeadMap(Map<String, String> headMap) {
        this.headMap = headMap;
    }

    @Override
    public String getHeader(String name) {
        return headMap.get(name);
    }
}
