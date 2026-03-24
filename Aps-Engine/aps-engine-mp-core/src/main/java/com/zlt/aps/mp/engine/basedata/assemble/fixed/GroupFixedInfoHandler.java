package com.zlt.aps.mp.engine.basedata.assemble.fixed;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 结构定点信息处理器
 *
 * @author ZLT
 * @date 20260328
 */
@Slf4j
@Component
public class GroupFixedInfoHandler {

    /**
     * 设置结构的指定机台信息
     *
     * @param context 排产上下文
     */
    public void setGroupPlanFixedCxMachineInfo(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupInfoMap = productionContext.getGroupProductionInfo();
        Map<String, CxMachineBaseInfoVo> allCxMachineInfoMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allGroupInfoMap) || CollectionUtils.isEmpty(allCxMachineInfoMap)) {
            return;
        }
        List<CxMachineBaseInfoVo> allCxMachineInfo = allCxMachineInfoMap.values().stream().collect(Collectors.toList());
        allGroupInfoMap.forEach((structName, groupInfo) -> {
            List<CxMachineBaseInfoVo> hasFixedList = allCxMachineInfo.stream().filter(singleMachineInfo -> singleMachineInfo.hasFixedMachine(groupInfo)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(hasFixedList)) {
                return;
            }
            groupInfo.setFixedCxMachineSet(hasFixedList.stream().map(CxMachineBaseInfoVo::getCxMachineCode).collect(Collectors.toSet()));
        });
    }

}
