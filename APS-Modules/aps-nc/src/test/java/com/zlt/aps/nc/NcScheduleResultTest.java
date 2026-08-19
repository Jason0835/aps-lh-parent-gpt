package com.zlt.aps.nc;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.nc.engine.service.NcEngineNewService;

@SpringBootTest
class NcScheduleResultTest {
    @Autowired
    private NcEngineNewService ncEngineService;

    @Test
    public void test() throws IOException {
        String factoryCode = "116";
        String scheduleDateStr = "2026-07-26";
        Date scheduleDate = DateUtils.parseDate(scheduleDateStr);
        ncEngineService.autoNcSchedule(factoryCode, scheduleDate);
    }

}
