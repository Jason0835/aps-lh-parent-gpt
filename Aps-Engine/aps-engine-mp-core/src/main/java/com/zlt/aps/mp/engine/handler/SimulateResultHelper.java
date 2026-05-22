package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Sets;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * 模拟排产后
 * 业务结果处理数据存储类
 * 用以辅助正式排产的处理
 *
 * @author ZLT
 * @date 20260522
 */
@Getter
public class SimulateResultHelper implements Serializable {
    /**
     * 续作Sku在没有高优先级情形下，仍然可以在首日排
     * 条件：续作Sku只有中优先级，且模拟排产时，又可以排得上
     */
    private Set<String> continueSkuCanFirstSet;

    /**
     * 设置续作Sku在没有高优先级需求可排净需求的Sku
     *
     * @param allContinueConditionSkuSet
     */
    public void setContinueSkuCanFirstInfo(Set<String> allContinueConditionSkuSet) {
        if (CollectionUtils.isEmpty(allContinueConditionSkuSet)) {
            continueSkuCanFirstSet = Collections.emptySet();
            return;
        }
        continueSkuCanFirstSet = Sets.newHashSet();
        continueSkuCanFirstSet.addAll(allContinueConditionSkuSet);
    }

    /**
     * 续作Sku是否可一开始排产只有净需求量而没有高优先级需求量
     *
     * @param materialDesc 续作Sku
     * @return
     */
    public boolean hasProductionFirstByContinueSku(String materialDesc) {
        if (StringUtils.isBlank(materialDesc)) {
            return false;
        }
        if (CollectionUtils.isEmpty(continueSkuCanFirstSet)) {
            return false;
        }
        return continueSkuCanFirstSet.contains(materialDesc);
    }

}
