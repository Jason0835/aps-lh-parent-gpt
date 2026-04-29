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
            //分组能排产的机台配置：固定1~固定4都算
            List<CxMachineBaseInfoVo> hasFixedList = allCxMachineInfo.stream().filter(singleMachineInfo -> singleMachineInfo.hasFixedMachine(groupInfo)).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(hasFixedList)) {
                groupInfo.setFixedCxMachineSet(hasFixedList.stream().map(CxMachineBaseInfoVo::getCxMachineCode).collect(Collectors.toSet()));
            }
            //20260427+ 因成型固定机台定义变化：固定1~固定3为选择机台的优先级；固定4位为分组能排产的机台配置
            List<CxMachineBaseInfoVo> hasPriorityFixedList = allCxMachineInfo.stream().filter(singleMachieInfo -> singleMachieInfo.hasPriorityFixedMachine(groupInfo)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(hasPriorityFixedList)) {
                return;
            }
            groupInfo.setPriorityFixedCxMachineSet(hasPriorityFixedList.stream().map(CxMachineBaseInfoVo::getCxMachineCode).collect(Collectors.toSet()));
        });
    }

}
