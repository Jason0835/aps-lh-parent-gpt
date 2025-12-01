package com.zlt.aps.cd15;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.cd15.engine.service.Cd15EngineService;

@SpringBootTest
class Cd15ScheduleResultTest {
	@Autowired
	private Cd15EngineService cd15EngineService;
	
	@Test
	public void test() throws IOException {
        String dateStr = "2025-10-11";
        Date scheduleDate = DateUtils.parseDate(dateStr);
	    cd15EngineService.autoCd15Schedule(scheduleDate);
	}

}
