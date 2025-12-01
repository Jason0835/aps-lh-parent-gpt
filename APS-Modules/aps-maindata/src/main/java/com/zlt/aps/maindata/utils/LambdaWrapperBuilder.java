package com.zlt.aps.maindata.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 构建LambdaQueryWrapper的查询Wrapper
 */
public class LambdaWrapperBuilder {

    @SafeVarargs
    public static <T> LambdaQueryWrapper<T> buildWrapperByFunction(List<T> list, SFunction<T, Object>... functions) {
        if (CollectionUtils.isEmpty(list) || functions == null || functions.length == 0) {
            return Wrappers.lambdaQuery();
        }
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        for (SFunction<T, Object> function : functions) {
            List<Object> itemList = list.stream().map(function).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            wrapper.in(CollectionUtils.isNotEmpty(itemList), function, itemList);
        }
        return wrapper;
    }
}
