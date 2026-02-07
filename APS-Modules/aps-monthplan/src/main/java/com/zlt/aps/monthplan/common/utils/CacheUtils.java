package com.zlt.aps.monthplan.common.utils;

import com.ruoyi.common.core.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;


/**
 * Cache工具类
 *
 * @author ruoyi
 */
@Slf4j
public class CacheUtils
{

  private static final CacheManager CACHE_MANAGER = SpringUtils.getBean(CacheManager.class);

  private static final String SYS_CACHE = "sys-cache";

  /**
   * 获取SYS_CACHE缓存
   *
   * @param key
   * @return
   */
  public static Object get(String key)
  {
    return get(SYS_CACHE, key);
  }

  /**
   * 获取SYS_CACHE缓存
   *
   * @param key
   * @param defaultValue
   * @return
   */
  public static Object get(String key, Object defaultValue)
  {
    Object value = get(key);
    return value != null ? value : defaultValue;
  }

  /**
   * 写入SYS_CACHE缓存
   *
   * @param key
   * @return
   */
  public static void put(String key, Object value)
  {
    put(SYS_CACHE, key, value);
  }

  /**
   * 获取缓存
   *
   * @param cacheName
   * @param key
   * @return
   */
  public static Object get(String cacheName, String key)
  {
    return getCache(cacheName).get(getKey(key));
  }

  /**
   * 获取缓存
   *
   * @param cacheName
   * @param key
   * @param defaultValue
   * @return
   */
  public static Object get(String cacheName, String key, Object defaultValue)
  {
    Object value = get(cacheName, getKey(key));
    return value != null ? value : defaultValue;
  }

  /**
   * 写入缓存
   *
   * @param cacheName
   * @param key
   * @param value
   */
  public static void put(String cacheName, String key, Object value)
  {
    getCache(cacheName).put(getKey(key), value);
  }

  /**
   * 获取缓存键名
   *
   * @param key
   * @return
   */
  private static String getKey(String key)
  {
    return key;
  }

  /**
   * 获得一个Cache，没有则显示日志。
   *
   * @param cacheName
   * @return
   */
  public static Cache getCache(String cacheName)
  {
    Cache cache = CACHE_MANAGER.getCache(cacheName);
    if (cache == null)
    {
      throw new RuntimeException("当前系统中没有定义“" + cacheName + "”这个缓存。");
    }
    return cache;
  }

}

