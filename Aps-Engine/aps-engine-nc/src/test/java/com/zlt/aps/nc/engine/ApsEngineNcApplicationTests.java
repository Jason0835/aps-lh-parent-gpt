package com.zlt.aps.nc.engine;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.nc.engine.service.NcEngineService;
import com.zlt.aps.nc.engine.vo.NcScheduleResultVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class ApsEngineNcApplicationTests {

    @Resource
    private NcEngineService ncEngineService;

    @Test
    public void test() {
        System.out.println("test start!!!!!!");
        System.out.println("test ending!!!!!!");
    }

    /**
     * 胎面自动排程测试
     */
    @Test
    public void autoNcScheduleTest() {
        ncEngineService.autoNcSchedule("2022-02-10");
    }

    /**
     * 手动均衡和重新设置生产顺序
     */
    @Test
    public void handEquilibriumAndProduceOrder() {
        ncEngineService.handEquilibriumAndProduceOrder("2021-06-29");
    }

    /**
     * 手动 同胶料合并生产
     */
    @Test
    public void handglueMerge() {
        ncEngineService.handGlueMerge("2022-02-10");
    }

    /**
     * 胎面插单测试
     */
    @Test
    public void inserNcOrderTest() throws Exception {
        NcScheduleResultVo scheduleVo = new NcScheduleResultVo();
        scheduleVo.setScheduleDate(DateUtils.parseDate("2021-06-29","yyyy-MM-dd"));
        scheduleVo.setLiningCode("HN0557");
        scheduleVo.setNightPlanQty(200D);
        scheduleVo.setDayPlanQty(300D);
        scheduleVo.setMachineId("64");
        scheduleVo.setNightProduceOrder(33);
        ncEngineService.inertNcOrder(scheduleVo);
    }
}
