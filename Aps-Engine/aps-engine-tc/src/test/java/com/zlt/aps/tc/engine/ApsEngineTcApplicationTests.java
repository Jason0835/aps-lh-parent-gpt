package com.zlt.aps.tc.engine;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.tc.engine.service.TcEngineService;
import com.zlt.aps.tc.engine.vo.TcScheduleResultVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class ApsEngineTcApplicationTests {

    @Resource
    private TcEngineService tcEngineService;
    @Resource
    private IncrementService incrementService;

    @Test
    public void test() {
        System.out.println("test start!!!!!!");
        System.out.println("test ending!!!!!!");
        System.out.println(incrementService.getSequence3("TM20210630"));
    }
    /**
     * 胎面自动排程测试
     */
    @Test
    public void autoTcScheduleTest() {
        tcEngineService.autoTcSchedule("2022-02-10");
    }

    /**
     * 手动均衡和重新设置生产顺序
     */
    @Test
    public void handEquilibriumAndProduceOrder() {
        tcEngineService.handEquilibriumAndProduceOrder("2023-11-03");
    }

    /**
     * 手动 同胶料合并生产
     */
    @Test
    public void handglueMerge() {
        tcEngineService.handGlueMerge("2022-02-10");
    }

    /**
     * 胎面插单测试
     */
    @Test
    public void inserTcOrderTest() throws Exception {
        TcScheduleResultVo scheduleVo = new TcScheduleResultVo();
        scheduleVo.setScheduleDate(DateUtils.parseDate("2021-06-29","yyyy-MM-dd"));
        scheduleVo.setSidewallCode("YHF0726");
        scheduleVo.setNightPlanQty(200D);
        scheduleVo.setDayPlanQty(300D);
        scheduleVo.setMachineId("64");
        scheduleVo.setDayProduceOrder(22);
        tcEngineService.inertTcOrder(scheduleVo);
    }
}
