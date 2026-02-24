package com.zlt.aps.mp.common.hashlize;


import com.zlt.aps.mp.common.utils.PubUtil;

import java.util.*;

public class Hashlize {
    /**
     * Hashlize 构造子注解。
     */
    public Hashlize() {
        super();
    }

    @SuppressWarnings("unchecked")
    public static Map hashlizeObjects(Object[] objs, IHashKey iHashKey) {
        if (objs == null || objs.length == 0) {
            return new HashMap<>();
        }

        if (iHashKey == null) {
            throw new RuntimeException("iHashKey cann't be null");
        }

        HashMap result = new HashMap();

        for (int i = 0; i < objs.length; i++) {
            String key = iHashKey.getKey(objs[i]);
            ArrayList al = (ArrayList) result.get(key);
            if (al == null) {
                al = new ArrayList();
                result.put(key, al);
            }
            al.add(objs[i]);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static Map hashlizeObjects(List objs, IHashKey iHashKey) {
        if (objs == null || objs.size() == 0) {
            return new HashMap<>();
        }

        if (iHashKey == null) {
            throw new RuntimeException("iHashKey cann't be null");
        }

        HashMap result = new HashMap();

        for (int i = 0; i < objs.size(); i++) {
            String key = iHashKey.getKey(objs.get(i));
            ArrayList al = (ArrayList) result.get(key);
            if (al == null) {
                al = new ArrayList();
                result.put(key, al);
            }
            al.add(objs.get(i));
        }

        return result;
    }

    /**
     * 添加有顺序的分组
     *
     * @param objs
     * @param iHashKey
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map hashlizeObjectsLinked(List objs, IHashKey iHashKey) {
        if (objs == null || objs.size() == 0) {
            return new HashMap<>();
        }

        if (iHashKey == null) {
            throw new RuntimeException("iHashKey cann't be null");
        }

        LinkedHashMap result = new LinkedHashMap();

        for (int i = 0; i < objs.size(); i++) {
            String key = iHashKey.getKey(objs.get(i));
            ArrayList al = (ArrayList) result.get(key);
            if (al == null) {
                al = new ArrayList();
                result.put(key, al);
            }
            al.add(objs.get(i));
        }

        return result;
    }

    public static Map hashlizeMap(Object[] objs, IHashKey iHashKey) {
        if (objs == null || objs.length == 0) {
            return new HashMap<>();
        }

        if (iHashKey == null) {
            throw new RuntimeException("iHashKey cann't be null");
        }

        HashMap result = new HashMap();

        for (int i = 0; i < objs.length; i++) {
            String key = iHashKey.getKey(objs[i]);
            result.put(key, objs[i]);
        }

        return result;
    }

    public static Map hashlizeMap(List objs, IHashKey iHashKey) {
        if (objs == null || objs.size() == 0) {
            return new HashMap<>();
        }

        if (iHashKey == null) {
            throw new RuntimeException("iHashKey cann't be null");
        }
        HashMap result = new HashMap();

        for (int i = 0; i < objs.size(); i++) {
            String key = iHashKey.getKey(objs.get(i));
            result.put(key, objs.get(i));
        }

        return result;
    }

    public static Map hashlizeObjects(Object[] objs, IHashKey iHashKey, ICombine iCombine) {
        if (objs == null || objs.length == 0) {
            return new HashMap<>();
        }

        if (iHashKey == null) {
            throw new RuntimeException("iHashKey cann't be null");
        }
        HashMap result = new HashMap();

        for (int i = 0; i < objs.length; i++) {
            String key = iHashKey.getKey(objs[i]);
            Object o = result.get(key);
            result.put(key, iCombine.combine(o, objs[i]));
        }
        return result;
    }

    public static Map hashlizeObjects(List objs, IHashKey iHashKey, ICombine iCombine) {
        if (objs == null || objs.size() == 0) {
            return new HashMap<>();
        }

        if (iHashKey == null) {
            throw new RuntimeException("iHashKey cann't be null");
        }
        HashMap result = new HashMap();

        int size = objs.size();
        for (int i = 0; i < size; i++) {
            String key = iHashKey.getKey(objs.get(i));
            Object o = result.get(key);
            result.put(key, iCombine.combine(o, objs.get(i)));
        }
        return result;
    }

    public static Set<String> hashlizeSet(List objs, IHashKey iHashKey) {
        if (PubUtil.isEmpty(objs)) {
            return new HashSet<>();
        }

        if (iHashKey == null) {
            throw new RuntimeException("iHashKey cann't be null");
        }

        Set<String> result = new HashSet();

        for (int i = 0; i < objs.size(); i++) {
            String key = iHashKey.getKey(objs.get(i));
            result.add(key);
        }
        return result;
    }

    public static List<String> hashlizeList(List objs, IHashKey iHashKey) {
        if (PubUtil.isEmpty(objs)) {
            return new ArrayList<>();
        }

        if (iHashKey == null) {
            throw new RuntimeException("iHashKey cann't be null");
        }

        List<String> result = new ArrayList();

        for (int i = 0; i < objs.size(); i++) {
            String key = iHashKey.getKey(objs.get(i));
            result.add(key);
        }
        return result;
    }

}
