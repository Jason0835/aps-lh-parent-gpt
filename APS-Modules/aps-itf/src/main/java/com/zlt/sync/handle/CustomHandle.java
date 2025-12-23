package com.zlt.sync.handle;

import com.alibaba.fastjson.JSONObject;

/**
 * 自定义方法接口
 */
public interface CustomHandle {

    public void handle(JSONObject jsonObject);
}
