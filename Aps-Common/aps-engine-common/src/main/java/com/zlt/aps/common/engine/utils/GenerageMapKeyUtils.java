package com.zlt.aps.common.engine.utils;


import com.ruoyi.common.utils.StringUtils;

/**
  * 组装mapkey工具类
  * @ClassName GenerageMapKeyUtils
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/29 19:54
  * @Version 1.0
**/
public class GenerageMapKeyUtils {

    /**
     * 分隔符
     */
    public static final String SPLT_CHAR=";";

    /**
     * 获取生成mapkey
     * @param keys
     * @return
     */
    public static String createMapKey(String ... keys){
        StringBuilder sb=new StringBuilder();
        for (String key: keys) {
            sb.append(StringUtils.trim(key)).append(SPLT_CHAR);
        }
        return sb.toString();
    }

    /**
     * 解析自定义生成key中的胎胚代码，只支持胎胚代码生成key，放在第一个位置的
     * @param key 胎胚代码为第一个入参的key
     * @return 解析出来为第一个字符串，否则返回空
     */
    public static String  getEmbryoCodeByCreateKey(String key){
        String embryoCode="";
        if(StringUtils.isNotEmpty(key)){
            String[] keys=key.split(SPLT_CHAR);
            if(StringUtils.isNotEmpty(keys)){
                embryoCode=keys[0];
            }
        }
        return embryoCode;
    }

}
