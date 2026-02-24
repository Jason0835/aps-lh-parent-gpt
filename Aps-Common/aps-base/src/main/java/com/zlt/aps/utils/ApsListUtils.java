package com.zlt.aps.utils;


import java.util.ArrayList;
import java.util.List;

/**
 * 一些常用的List方法，比如分割
 */
public class ApsListUtils {

    /**
     * 按长度分割
     * @param list list
     * @param splitLength 分割长度
     * @return 分割好的List
     * @param <V>
     */
    public static <V> List<List<V>> getSplitList(List<V> list,int splitLength) {

        List<List<V>> partitions = new ArrayList<>();
        if (list.size() <= splitLength){
            partitions.add(list);
            return partitions;
        }
        for (int i = 0; i < list.size(); i += splitLength) {
            int end = Math.min(list.size(), i + splitLength);
            List<V> partition = list.subList(i, end);
            partitions.add(partition);
        }
        // 输出分割后的结果
       return partitions;

    }
}
