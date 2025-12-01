package com.zlt.mix.schedule.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.controller.GlueScheduleResultController;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEngineService;

@SpringBootTest
//@MapperScan({"com.zlt.mix.schedule.engine.mapper"})
public class GlueScheduleServiceTest {
    @Autowired
    private GlueScheduleEngineService glueScheduleEngineService;
    @Autowired
    private GlueScheduleResultController glueScheduleResultController;

    @Test
    public void publish() {
        GlueScheduleResult glueScheduleResult = new GlueScheduleResult();
        glueScheduleResult.setIds("6283472");
        glueScheduleResultController.publish(glueScheduleResult);
    }
    
//    @Test
    public void autoPlan() {
        glueScheduleEngineService.autoGlueSchedule(DateUtils.dateTime("yyyyMMdd", "20250820"), "M2");
    }

}
