package com.zlt.aps.mp.engine.daylimit;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.EarliestConclusionLhGroupHelper;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 换模限制处理器
 * 日换模次数限制
 *
 * @author ZLT
 * @date 20260122
 */
public class ChangeMouldLimitHandler {

    /**
     * 换模次数控制处理，调整其上机日
     *
     * @param context         排产上下文
     * @param lhGroup         收尾硫化组，含有上机日、收尾日
     * @param doubleMouldList 使用的模具
     */
    public static void changeMouldLimit(Context context, EarliestConclusionLhGroupHelper lhGroup, List<ProductionMouldInfoVo> doubleMouldList) {
        if (null == lhGroup || CollectionUtils.isEmpty(doubleMouldList)) {
            return;
        }
        Integer startDay = lhGroup.getClosingDay();
        Integer endDay = lhGroup.getEndDay();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        DayCapacityLimitVo dayCapacityLimit = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Set<Integer> hasChangeMouldDaySet = dayCapacityLimit.getHasChangeMouldProductionDay(context);
        //达到换模次数限制
        if (CollectionUtils.isEmpty(hasChangeMouldDaySet)) {
            lhGroup.updateProductionDateRange(null, null);
            return;
        }
        //可进行换模
        if (hasChangeMouldDaySet.contains(startDay)) {
            return;
        }
        //开始时间需要推迟 提取在startDay后，首个最小的日期
        Set<Integer> afterTheoryChangeDayList = hasChangeMouldDaySet.stream().filter(singleDay -> singleDay > startDay).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(afterTheoryChangeDayList)) {
            lhGroup.updateProductionDateRange(null, null);
            return;
        }
        List<Integer> resultList = new ArrayList<>(afterTheoryChangeDayList);
        resultList.sort(Comparator.comparing(Integer::intValue));
        Integer realChangeDay = resultList.get(BigDecimal.ZERO.intValue());
        if (realChangeDay > endDay) {
            lhGroup.updateProductionDateRange(null, null);
            return;
        }
        lhGroup.updateProductionDateRange(realChangeDay, endDay);
        return;
    }
}
