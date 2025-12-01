package com.zlt.mix.schedule.engine.service.basicdata.impl;

import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.schedule.engine.mapper.MixingTimeEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.MixingTimeEngineService;
import com.zlt.mix.setting.api.domain.entity.MixingTime;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 引擎炼胶间隔时间相关ServiceImpl
 */
@Service
public class MixingTimeEngineServiceImpl implements MixingTimeEngineService {
    @Resource
    private MixingTimeEngineMapper mixingTimeEngineMapper;

    /**
     * 加载胶料间隔时间，按胶料 + 机台分组
     */
    @Override
    public Map<String, Long> mapMixingIntervalTime(String mixArea) {
        MixingTime query = new MixingTime();
        query.setMixArea(mixArea);
        return queryMixingIntervalTimeMap(query);
    }

    /**
     * 加载胶料间隔时间Map，按胶料 + 机台分组
     */
    private Map<String, Long> queryMixingIntervalTimeMap(MixingTime query) {
        return mixingTimeEngineMapper.listMixingTime(query).stream()
                .collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getGlue(), v.getMachineCode()), MixingTime::getIntervalTime, (v1, v2) -> v1));
    }

    /**
     * 查询对应密炼区+胶料+机台的炼胶间隔时间
     *
     * @param mixArea     密炼区
     * @param glue        胶料
     * @param machineCode 机台
     * @return 炼胶间隔时间，如果无则为空
     */
    @Override
    public BigDecimal getIntervalTime(String mixArea, String glue, String machineCode) {
        MixingTime query = new MixingTime();
        query.setMixArea(mixArea);
        query.setGlue(glue);
        query.setMachineCode(machineCode);

        Map<String, Long> map = queryMixingIntervalTimeMap(query);
        Long intervalTime = map.get(GenerageMapKeyUtils.createMapKey(glue, machineCode));
        return intervalTime != null ? BigDecimal.valueOf(intervalTime) : null;
    }
}
