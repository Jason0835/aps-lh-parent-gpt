package com.zlt.aps.factory.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * Sku排产计数器
 * 记录Sku首次排产的顺序
 *
 * @author ZLT
 * @date 2025260129
 */
@Slf4j
public class SkuProductionCounter {
    /**
     * 已排产Sku集合
     */
    private Set<String> plannedSkuSet;
    /**
     * 排产Sku顺序
     */
    private List<String> plannedSkuSortList;
    /**
     * 构建Sku的排产顺序值
     */
    private Map<String, Integer> skuProductionSortMap;

    /**
     * 构建初始化
     *
     * @return
     */
    public static SkuProductionCounter buildInit() {
        SkuProductionCounter init = new SkuProductionCounter();
        init.plannedSkuSet = new HashSet<>();
        init.plannedSkuSortList = new ArrayList<>();
        return init;
    }

    /**
     * 增加排产Sku信息
     *
     * @param materialDesc 物料描述
     */
    public void addProductionSku(String materialDesc) {
        if (StringUtils.isBlank(materialDesc)) {
            return;
        }
        if (plannedSkuSet.contains(materialDesc)) {
            return;
        }
        plannedSkuSet.add(materialDesc);
        plannedSkuSortList.add(materialDesc);
    }

    /**
     * 根据物料描述，获取其排产顺序值
     *
     * @param materialDesc 物料描述
     * @return
     */
    public Integer getSkuProductionSort(String materialDesc) {
        if (null == skuProductionSortMap) {
            buildSkuProductionSort();
        }
        if (CollectionUtils.isEmpty(skuProductionSortMap)) {
            return null;
        }
        return skuProductionSortMap.get(materialDesc);
    }

    /**
     * 构建各Sku的排产顺序
     */
    private void buildSkuProductionSort() {
        if (CollectionUtils.isEmpty(plannedSkuSortList)) {
            skuProductionSortMap = Collections.emptyMap();
            return;
        }
        skuProductionSortMap = new HashMap<>(plannedSkuSortList.size());
        Integer sort = BigDecimal.ONE.intValue();
        for (String materialDesc : plannedSkuSortList) {
            if (skuProductionSortMap.containsKey(materialDesc)) {
                continue;
            }
            skuProductionSortMap.put(materialDesc, sort);
            sort = sort + BigDecimal.ONE.intValue();
        }
    }

}
