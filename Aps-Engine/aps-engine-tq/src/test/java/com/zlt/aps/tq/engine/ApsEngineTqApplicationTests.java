package com.zlt.aps.tq.engine;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.tq.engine.service.TqEngineService;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class ApsEngineTqApplicationTests {

    @Resource
    private TqEngineService tqEngineService;

    @Test
    public void test() {
        System.out.println("test start!!!!!!");
        System.out.println("test ending!!!!!!");
    }


    /**
     * 胎面自动排程测试
     */
    @Test
    public void autoTqScheduleTest() {
        tqEngineService.autoTqSchedule("2022-02-10");
    }

    /**
     * 胎面插单测试
     */
    @Test
    public void inserTqOrderTest() throws Exception{
        TqScheduleResultVo scheduleVo = new TqScheduleResultVo();
        scheduleVo.setScheduleDate(DateUtils.parseDate("2021-06-29","yyyy-MM-dd"));
        scheduleVo.setBeadCode("HC1913");
        scheduleVo.setNightPlanQty(200D);
        scheduleVo.setDayPlanQty(300D);
        tqEngineService.inertTqOrder(scheduleVo);
    }
}
