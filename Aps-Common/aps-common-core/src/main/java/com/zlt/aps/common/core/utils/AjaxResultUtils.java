package com.zlt.aps.common.core.utils;

import java.util.Collections;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * AjaxResult工具
 * 
 * @author zlt
 *
 */
public class AjaxResultUtils {
	/**
	 * 获取ajaxResult返回的列表，会将列表元素解析成指定的类型
	 * 
	 * @param <T>
	 * @param ajaxResult
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getList(AjaxResult ajaxResult, Class<T> clazz) {
		Object resultData = ajaxResult.get(AjaxResult.DATA_TAG);
		if (resultData instanceof List) {
			return JSONArray.parseArray(JSONArray.toJSONString(resultData), clazz);
		}
		return Collections.EMPTY_LIST;
	}
}
