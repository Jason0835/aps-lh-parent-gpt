package com.zlt.aps.mp.engine.basedata.assemble.appoint;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.vo.GroupAppointProductionInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分组信息对象指定生产配置数据加载
 *
 * @author ZLT
 * @date 20260713
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupAppointDataHandler {

    private final MonthProductionDataService monthProductionDataService;

    /**
     * 加载特殊的指定信息，用以满足
     * 特殊场景业务
     *
     * @param context 排产上下文
     * @return
     */
    public void loadAppointInfo(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //获取配置信息
        List<GroupAppointProductionInfoVo> allConfigurationList = monthProductionDataService.getMonthAppointProductionInfo(context);
        if (CollectionUtils.isEmpty(allConfigurationList)) {
            productionContext.getBaseDataContainer().setAppointConfiguration(Collections.emptyList());
            return;
        }
        //有效配置
        List<GroupAppointProductionInfoVo> effectiveConfiguration = allConfigurationList.stream().filter(single -> single.isEffectiveConfiguration(productionContext)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveConfiguration)) {
            productionContext.getBaseDataContainer().setAppointConfiguration(Collections.emptyList());
            return;
        }
        productionContext.getBaseDataContainer().setAppointConfiguration(effectiveConfiguration);
        return;
    }

}
