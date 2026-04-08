package com.zlt.aps.mdm.api.enums;

import com.alibaba.fastjson.JSONObject;

/**
 * 公共枚举类型接口
 *
 * @author APS Team
 */
public interface PublicEnum {

    String getCode();

    String getName();

    /**
     * 前端json格式
     *
     * @return JSON对象
     */
    default JSONObject getJSON() {
        JSONObject map = new JSONObject();
        map.put("code", getCode());
        map.put("name", getName());
        return map;
    }
}
