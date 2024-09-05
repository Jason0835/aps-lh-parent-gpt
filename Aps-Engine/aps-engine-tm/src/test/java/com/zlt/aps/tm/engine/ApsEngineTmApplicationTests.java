package com.zlt.aps.tm.engine;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.common.engine.domain.AutoScheduleLog;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.tm.engine.service.TmEngineService;
import com.zlt.aps.tm.engine.vo.TmScheduleResultVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class ApsEngineTmApplicationTests {

    @Resource
    private TmEngineService tmEngineService;
    @Resource
    private IncrementService incrementService;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Test
    public void test() {
        System.out.println("test start!!!!!!");
        System.out.println("test ending!!!!!!");
        System.out.println(incrementService.getSequence3("TM20210630"));
    }

    @Test
    public void test11() {
//        autoScheduleLogService.insertScheduleLog("1","2","33","title","666666");
    }

    /**
     * 胎面自动排程测试
     */
    @Test
    public void autoTmScheduleTest() {
        tmEngineService.autoTmSchedule("2022-02-10");
    }

    /**
     * 手动均衡和重新设置生产顺序
     */
    @Test
    public void handEquilibriumAndProduceOrder() {
        tmEngineService.handEquilibriumAndProduceOrder("2022-04-20");
    }

    /**
     * 手动 同胶料合并生产
     */
    @Test
    public void handGlueMerge() {
        tmEngineService.handGlueMerge("2022-02-10");
    }

    /**
     * 胎面插单测试
     */
    @Test
    public void inserTmOrderTest() throws Exception{
        TmScheduleResultVo scheduleVo = new TmScheduleResultVo();
        scheduleVo.setScheduleDate(DateUtils.parseDate("2022-02-11","yyyy-MM-dd"));
        scheduleVo.setTreadCode("HT8021-1339P");
        scheduleVo.setNightPlanQty(200D);
        scheduleVo.setDayPlanQty(300D);
        scheduleVo.setMachineId("20080");
        scheduleVo.setDayProduceOrder(10);
        scheduleVo.setNightProduceOrder(2);
        tmEngineService.inertTmOrder(scheduleVo);
    }
}
