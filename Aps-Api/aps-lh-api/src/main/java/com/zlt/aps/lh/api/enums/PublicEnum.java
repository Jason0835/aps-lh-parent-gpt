package com.zlt.aps.lh.api.enums;

import com.alibaba.fastjson.JSONObject;


/**
 * @author xh
 * @version 1.0
 * @Description 公共枚举类型接口
 */
public interface PublicEnum {

    String getCode();

    String getName();

    /**
     * 前端json格式
     *
     * @return
     */
    default JSONObject getJSON() {
        JSONObject map = new JSONObject();
        map.put("code", getCode());
        map.put("name", getName());
        return map;
    }
}
