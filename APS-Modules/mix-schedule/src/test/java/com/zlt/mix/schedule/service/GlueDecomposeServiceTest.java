package com.zlt.mix.schedule.service;

import javax.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.engine.service.decompose.DecomposeEngineService;

@SpringBootTest
public class GlueDecomposeServiceTest {
    @Resource
    private DecomposeEngineService decomposeEngineService;

    @Test
    public void autoPlan() {
        decomposeEngineService.decomposePlan(DateUtils.dateTime("yyyyMMdd", "20250820"), "M2");
    }

}
