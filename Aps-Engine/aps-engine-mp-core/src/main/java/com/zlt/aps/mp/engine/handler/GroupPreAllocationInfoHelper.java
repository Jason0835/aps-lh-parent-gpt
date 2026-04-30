package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * 第一轮模拟排产后，对新增预排的分组进行排产分析对象
 *
 * @author ZLT
 * @date 20260430
 */
@Getter
public class GroupPreAllocationInfoHelper implements Serializable {
    /**
     * 分组名
     */
    private String groupName;
    /**
     * 分组对象
     */
    private ProductionPlanGroupInfo groupInfo;
    /**
     * 排产日集合
     */
    private Set<Integer> productionDateSet;
    /**
     * 停产日集合(包含机台的停产日)
     */
    private Set<Integer> topDaysSet;

    public GroupPreAllocationInfoHelper(String groupName, ProductionPlanGroupInfo groupInfo, Set<Integer> productionDateSet, Set<Integer> topDaysSet) {
        this.groupName = groupName;
        this.groupInfo = groupInfo;
        this.productionDateSet = productionDateSet;
        this.topDaysSet = topDaysSet;
    }

    /**
     * 判断是否有间断排产
     *
     * @return
     */
    public boolean hasDiscontinueProduction() {
        if (StringUtils.isBlank(groupName) || null == groupInfo || CollectionUtils.isEmpty(productionDateSet)) {
            return false;
        }
        if (!groupName.equals(groupInfo.getGroupName())) {
            return false;
        }
        Set<Integer> stopInfo = Optional.ofNullable(topDaysSet).orElse(Collections.emptySet());
        List<Integer> productionDayList = Lists.newArrayList(productionDateSet);
        //从小到大排序
        productionDayList.sort(Comparator.comparing(Integer::intValue));
        Integer startDay = productionDayList.get(BigDecimal.ZERO.intValue());
        Integer endDay = productionDayList.get(productionDateSet.size() - BigDecimal.ONE.intValue());
        for (int index = startDay; index <= endDay; index++) {
            if (!stopInfo.contains(index) && !productionDateSet.contains(index)) {
                return true;
            }
        }
        return false;
    }

}
