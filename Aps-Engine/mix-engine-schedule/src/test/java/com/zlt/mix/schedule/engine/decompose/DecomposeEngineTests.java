package com.zlt.mix.schedule.engine.decompose;

import com.zlt.mix.schedule.engine.service.decompose.DecomposeEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;

@SpringBootTest
class DecomposeEngineTests {

    @Resource
    private DecomposeEngineService decomposeEngineService;

    /**
     * 根据终炼胶的汇总计划分解出对应的母炼胶的日计划
     */
    @Test
    public void decomposePlan() throws Exception{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        decomposeEngineService.decomposePlan(sdf.parse("2024-04-10"), "M2");
    }

}
