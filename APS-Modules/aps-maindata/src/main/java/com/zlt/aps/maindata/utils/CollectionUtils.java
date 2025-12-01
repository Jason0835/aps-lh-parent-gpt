package com.zlt.aps.maindata.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chen
 */
public class CollectionUtils {

    /**
     * 将一个 List 集合拆分成多个指定大小的子集合
     *
     * @param list       原始的 List 集合
     * @param subListSize 每个子集合的大小
     * @param <T>        集合中元素的类型
     * @return 包含多个子集合的 List
     */
    public static <T> List<List<T>> splitList(List<T> list, int subListSize) {
        if (list == null || list.isEmpty() || subListSize <= 0) {
            return new ArrayList<>();
        }

        List<List<T>> result = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += subListSize) {
            int end = Math.min(i + subListSize, size);
            result.add(new ArrayList<>(list.subList(i, end)));
        }
        return result;
    }
}
