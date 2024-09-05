package com.ruoyi.system.utils;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.system.service.ISysDictTypeService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典工具类，多国语言
 *
 * @author ruoyi
 * @modify linbn 多国语言字典 201118
 */
public class DictUtils {
    /**
     * 设置字典缓存
     *
     * @param key       参数键
     * @param dictDatas 字典数据列表
     */
    public static void setDictCache(String key, List<SysDictData> dictDatas) {

        Map<String, List<SysDictData>> datas =
                dictDatas.stream().collect(
                        Collectors.groupingBy(SysDictData::getLocale)
                );
        Iterator<Map.Entry<String, List<SysDictData>>> iterator = datas.entrySet().iterator();
        int i = 0;//计算首次
        RedisService redisService = SpringUtils.getBean(RedisService.class);
        while (iterator.hasNext()) {
            Map.Entry entry = iterator.next();
            String newkey = getCacheDefaultKey(entry.getKey() + ":" + key);
            redisService.setCacheObject(newkey, JSON.toJSONString(entry.getValue()));
            redisService.persistKey(newkey);

            if (i == 0) {
                //默认的key 存一份字典
                newkey = getCacheDefaultKey(key);
                redisService.setCacheObject(newkey, JSON.toJSONString(entry.getValue()));
                redisService.persistKey(newkey);
            }
        }
    }

    /**
     * 获取字典缓存
     *
     * @param key 参数键
     * @return dictDatas 字典数据列表
     */
    public static List<SysDictData> getDictCache(String key) {

        RedisService redisService = SpringUtils.getBean(RedisService.class);
        //找用户的语言包
        String cacheObj =  Convert.toStr(redisService.getCacheObject(getCacheLocaleKey(key)));
        List<SysDictData> dictDatas = null;
        if (StringUtils.isNotNull(cacheObj)) {
            dictDatas = JSON.parseArray(cacheObj, SysDictData.class);
            return dictDatas;
        }else {
            //缓存为空，查询数据库存入缓存
            SysDictDataMapper dictDataMapper = SpringUtils.getBean(SysDictDataMapper.class);
            dictDatas = dictDataMapper.selectDictDataByType(key);
            if (StringUtils.isNotEmpty(dictDatas))
            {
                setDictCache(key, dictDatas);
                return dictDatas;
            }
        }

        //找默认的语言包
        cacheObj =  Convert.toStr(redisService.getCacheObject(getCacheDefaultKey(key)));
        if (StringUtils.isNotNull(cacheObj)) {
            dictDatas = JSON.parseArray(cacheObj, SysDictData.class);
            return dictDatas;
        }else {
            //缓存为空，查询数据库存入缓存
            SysDictDataMapper dictDataMapper = SpringUtils.getBean(SysDictDataMapper.class);
            dictDatas = dictDataMapper.selectDictDataByType(key);
            if (StringUtils.isNotEmpty(dictDatas))
            {
                setDictCache(key, dictDatas);
                return dictDatas;
            }
        }
        return null;
    }

    /***
     * 读取字典类型标签
     * @param dictType
     * @param dictValue
     * @return
     */
    public static String getLabel(String dictType, String dictValue) {
        List<SysDictData> data = getDictCache(dictType);
        String label = null;
        if (StringUtils.isNotNull(data)) {
            Optional<SysDictData> result = data.stream().filter(
                    item -> StringUtils.equals(item.getDictValue(), dictValue)
            ).findFirst();

            label = result.isPresent() ? result.get().getDictLabel() : dictValue;
        }

        return label;
    }

    /**
     * 清空字典缓存
     */
    public static void clearDictCache() {
        Collection<String> keys = SpringUtils.getBean(RedisService.class).keys(Constants.SYS_DICT_KEY + "*");
        SpringUtils.getBean(RedisService.class).deleteObject(keys);
    }

    public static void initCache() {
        DictUtils.clearDictCache();
        SpringUtils.getBean(ISysDictTypeService.class).init();
    }

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    public static String getCacheDefaultKey(String configKey) {
        return Constants.SYS_DICT_KEY + configKey;
    }

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    public static String getCacheLocaleKey(String configKey) {
        Locale locale = I18nUtil.getLocaleFromRedis();
        String key = locale.toString() + ":" + configKey;
        return getCacheDefaultKey(key);
    }
}
