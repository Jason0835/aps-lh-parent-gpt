package com.zlt.aps.mp.engine.basedata.assemble.cyclegroup;

import com.zlt.aps.mp.engine.domain.vo.MonthCycleGroupInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 周期结构数据相关加载处理
 *
 * @author ZLT
 * @date 20260403
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CycleGroupDataHandler {

    /**
     * 主数据数据提供接口
     */
    private final ProductionMdmDataService dataService;

    /**
     * 加载月周期排产清单
     *
     * @param productionContext
     */
    public Set<String> getMonthCycleGroupInfo(TbrProductionContext productionContext) {
        List<MonthCycleGroupInfoVo> monthProductionCycleGroupList = dataService.getMonthCycleGroupList(productionContext);
        if (CollectionUtils.isEmpty(monthProductionCycleGroupList)) {
            return Collections.emptySet();
        }
        Set<String> monthProductionSet = monthProductionCycleGroupList.stream().map(MonthCycleGroupInfoVo::getGroupName).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(monthProductionSet)) {
            return Collections.emptySet();
        }
        return monthProductionSet;
    }

}
