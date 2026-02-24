package com.tlt.aps.utils;


import com.ruoyi.common.utils.StringUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;

/**
  * 组装mapkey工具类
  * @ClassName GenerageMapKeyUtils
  * @Description
  * @Author Joran.Zhang
  * @Date 2021/6/29 19:54
  * @Version 1.0
**/
public class GenerageMapKeyUtils {

    /**
     * 分隔符
     */
    public static final String SPLT_CHAR="";

    /**
     * 获取生成mapkey
     * @param keys
     * @return
     */
    public static String createMapKey(String ... keys){
        StringBuilder sb=new StringBuilder();
        for (String key: keys) {
            sb.append(StringUtils.trim(ObjectUtils.isEmpty(key) ? "" : key)).append(SPLT_CHAR);
        }
        return sb.toString();
    }

    /**
     * 根据传入参数获取拼接后的key
     * @param keys 需要拼接的key
     * @return 结果
     */
    public static String createMapKey(Object ... keys) {
        StringBuilder sb=new StringBuilder();
        for (Object key : keys) {
            if (key instanceof String) {
                sb.append(ObjectUtils.isEmpty(key) ? "" : StringUtils.trim(key.toString())).append(SPLT_CHAR);
            }else if (key instanceof BigDecimal) {
                sb.append(ObjectUtils.isEmpty(key) ? BigDecimal.ZERO : key).append(SPLT_CHAR);
            }else if (key instanceof Long) {
                sb.append(ObjectUtils.isEmpty(key) ? BigDecimal.ZERO.longValue() : key).append(SPLT_CHAR);
            }else if (key instanceof Integer) {
                sb.append(ObjectUtils.isEmpty(key) ? BigDecimal.ZERO.intValue() : key).append(SPLT_CHAR);
            }else if (key instanceof Double) {
                sb.append(ObjectUtils.isEmpty(key) ? BigDecimal.ZERO.doubleValue() : key).append(SPLT_CHAR);
            }else {
                sb.append(ObjectUtils.isEmpty(key) ? "" : StringUtils.trim(key.toString())).append(SPLT_CHAR);
            }
        }
        return sb.toString();
    }
}
