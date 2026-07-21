package com.zlt.aps.gsq.engine;

import com.zlt.aps.gsq.engine.service.GsqEngineService;
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
     * 钢丝圈自动排程测试（6班制新架构）
     */
    @Test
    public void autoGsqScheduleTest() {
        gsqEngineService.autoGsqSchedule("2022-02-10", "默认分厂");
    }
}
