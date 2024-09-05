package com.zlt.aps.gsq.engine;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.gsq.engine.service.GsqEngineService;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class ApsEngineGsqApplicationTests {

    @Resource
    private GsqEngineService gsqEngineService;

    @Test
    public void test() {
        System.out.println("test start!!!!!!");
        System.out.println("test ending!!!!!!");

    }

    /**
     * 胎面自动排程测试
     */
    @Test
    public void autoGsqScheduleTest() {
        gsqEngineService.autoGsqSchedule("2022-02-10");
    }

    /**
     * 胎面插单测试
     */
    @Test
    public void inserGsqOrderTest() throws Exception{
        GsqScheduleResultVo scheduleVo = new GsqScheduleResultVo();
        scheduleVo.setScheduleDate(DateUtils.parseDate("2021-06-29","yyyy-MM-dd"));
        scheduleVo.setSteelRingCode("HR0468");
        scheduleVo.setMidPlanQty(400D);
        scheduleVo.setNightPlanQty(200D);
        scheduleVo.setDayPlanQty(300D);
        scheduleVo.setMachineId("64");
        gsqEngineService.inertGsqOrder(scheduleVo);
    }
}
