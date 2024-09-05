package com.zlt.aps.common.engine.utils;

import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Gim
 */
public class CollectionUtil extends CollectionUtils {
    /**
     *
     * @param keyMapper key
     * @param <T> list中的对象类型
     * @param <K> key类型
     * @return map
     */
    public static <T, K> Map<K, T> toMap(Collection<T> list, Function<? super T, ? extends K> keyMapper) {
        Map<K, T> map = new HashMap<>();
        if (list == null || keyMapper == null) {
            return map;
        }
        map = list.stream().collect(Collectors.toMap(keyMapper, Function.identity(), (oldObj, newObj)-> newObj));
        return map;
    }

    public static <J, K> HashMap<J, List<K>> toMapList(Collection<K> co, PropertyGetter<K, J> ih) {
        LinkedHashMap<J, List<K>> hm = new LinkedHashMap<>();
        for (K c : co) {
            J key = ih.getProperty(c);
            List<K> exists = hm.computeIfAbsent(key, k -> new ArrayList<>());
            exists.add(c);
        }
        return hm;
    }

    public static <P, T> ArrayList<P> propertiesToList(List<T> tList, PropertyGetter<T, P> propertyGetter) {
        if (null == tList || tList.isEmpty()) {
            return new ArrayList<>();
        }
        ArrayList<P> ret = new ArrayList<>(tList.size());
        for (T t : tList) {
            ret.add(propertyGetter.getProperty(t));
        }
        return ret;
    }
	
	/**
	 * 过滤列表
	 * 
	 * @param list      待过滤列表
	 * @param binarySeq 需要保留的下标二进制码
	 * @return
	 */
	public static <T> List<T> filterList(List<T> list, int binarySeq) {
		List<T> resultList = new ArrayList<>();
		if (binarySeq <= 0) {
			return resultList;
		}
		for (int i = 0, size = list.size(); i < size; i++) {
			T stock = list.get(i);
			if (binarySeq != 0 && (binarySeq & (1 << i)) != 0) {// 将高值二进制序号解析成下标
				resultList.add(stock);
			}
		}
		return resultList;
	}
}
