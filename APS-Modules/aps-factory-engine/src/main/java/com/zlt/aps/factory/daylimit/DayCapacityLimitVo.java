package com.zlt.aps.factory.daylimit;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日产能控制对象
 * 排产日、最大产能上限、最低产能，产能比例
 *
 * @author ZLT
 * @date 20250106
 */
@Getter
public class DayCapacityLimitVo implements Serializable {
    /**
     * 日产能控制信息
     * key=排产日 : value=日排产控制信息
     */
    private Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap;

    /**
     * 构建日产能控制对象
     *
     * @param dayCapacityLimitMap
     */
    public DayCapacityLimitVo(Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap) {
        this.dayCapacityLimitMap = dayCapacityLimitMap;
    }

    /**
     * 更新日排产信息
     *
     * @param dayCapacityLimitMap
     */
    public void updateWholeDayLimitInfo(Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap) {
        this.dayCapacityLimitMap = dayCapacityLimitMap;
    }

    /**
     * 确认切换结构的上机排产日
     *
     * @param context               排产上下文
     * @param theoryChangeDay       理论的切换结构日
     * @param changeGroupName       切换后的结构
     * @param selectedCxMachineInfo 选中的机台
     * @param hasProductionDaySet   可排产日集合
     * @return
     */
    public Integer confirmStartDayByChangeGroup(Context context, Integer theoryChangeDay, String changeGroupName, CxMachineBaseInfoVo selectedCxMachineInfo, Set<Integer> hasProductionDaySet) {
        //没有切换结构
        if (!selectedCxMachineInfo.isChangeGroup(theoryChangeDay, changeGroupName)) {
            return theoryChangeDay;
        }
        Set<Integer> hasChangeGroupSet = getHasChangeGroupProductionDay(context);
        if (CollectionUtils.isEmpty(hasChangeGroupSet)) {
            return null;
        }
        if (hasChangeGroupSet.contains(theoryChangeDay)) {
            return theoryChangeDay;
        }
        //提取在theoryChangeDay后，首个最小的日期
        Set<Integer> afterTheoryChangeDayList = hasChangeGroupSet.stream().filter(singleDay -> singleDay > theoryChangeDay).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(afterTheoryChangeDayList)) {
            return null;
        }
        Set<Integer> resultSet = hasChangeGroupSet.stream().filter(afterTheoryChangeDayList::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(resultSet)) {
            return null;
        }
        List<Integer> resultList = new ArrayList<>(resultSet);
        resultList.sort(Comparator.comparing(Integer::intValue));
        return resultList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 获取能切换分组的排产日集合
     *
     * @param context 排产上下文
     * @return
     */
    public Set<Integer> getHasChangeGroupProductionDay(Context context) {
        //没有限制时，所有的可排产日
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return context.getProductionDay();
        }
        List<DayCapacityLimitHelper> hasChangeGroupList = dayCapacityLimitMap.values().stream().filter(singleDay -> singleDay.getLeftOverUsedChangeGroupQty() > BigDecimal.ONE.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasChangeGroupList)) {
            return Collections.emptySet();
        }
        return hasChangeGroupList.stream().map(DayCapacityLimitHelper::getProductionDay).collect(Collectors.toSet());
    }

    /**
     * 增加每日结构切换使用次数
     *
     * @param context              排产上下文
     * @param changeProductionDate 切换日
     * @param cxMachineCode        成型机台
     * @param groupName            分组名
     */
    public void addChangeGroupNameUsedQty(Context context, Integer changeProductionDate, String cxMachineCode, String groupName) {
        if (!isEffectiveParam(changeProductionDate, cxMachineCode, groupName)) {
            return;
        }
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(changeProductionDate);
        if (null == dayLimit) {
            return;
        }
        dayLimit.addChangeGroupUsedQty(cxMachineCode, groupName);
    }

    /**
     * 减少每日结构切换使用次数
     *
     * @param context              排产上下文
     * @param changeProductionDate 切换日
     * @param cxMachineCode        成型机台
     * @param groupName            分组名
     */
    public void deductionChangeGroupNameUsedQty(Context context, Integer changeProductionDate, String cxMachineCode, String groupName) {
        if (!isEffectiveParam(changeProductionDate, cxMachineCode, groupName)) {
            return;
        }
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(changeProductionDate);
        if (null == dayLimit) {
            return;
        }
        dayLimit.deductionChangeGroupUsedQty(cxMachineCode, groupName);
    }

    /**
     * 是否有效参数
     * <p>
     * true 有效 false 无效
     *
     * @param changeProductionDate 切换日
     * @param cxMachineCode        成型机台
     * @param groupName            分组计划名(TBR结构名)
     * @return
     */
    private boolean isEffectiveParam(Integer changeProductionDate, String cxMachineCode, String groupName) {
        if (null == changeProductionDate) {
            return false;
        }
        if (StringUtils.isBlank(groupName) || StringUtils.isBlank(cxMachineCode)) {
            return false;
        }
        return true;
    }
}
