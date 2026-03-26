package com.zlt.aps.mp.engine.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Set;

/**
 * 值合并集合处理工具类
 *
 * @author ZLT
 * @date 20260325
 */
@Slf4j
public class CollectValueUtils {

    /**
     * 将addContent内容，按splitFlag分隔
     * 去除前后空格加入collectInfo中
     *
     * @param collectInfo 已有的内容集合
     * @param addContent  需要加入的集合
     */
    public static void addSingleValueToCollect(Set<String> collectInfo, String addContent, String splitFlag) {
        if (null == collectInfo || StringUtils.isBlank(addContent)) {
            return;
        }
        if (StringUtils.isBlank(splitFlag)) {
            collectInfo.add(addContent);
            return;
        }
        Arrays.stream(addContent.split(splitFlag))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .forEach(collectInfo::add);
        return;
    }
}
