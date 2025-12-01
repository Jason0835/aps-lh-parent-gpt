package com.zlt.aps.cd90;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.cd90.engine.service.Cd90EngineService;

@SpringBootTest
class Cd90ScheduleResultTest {
	@Autowired
	private Cd90EngineService cd90EngineService;
	
	@Test
	public void test() throws IOException {
        String dateStr = "2025-07-02";
        Date scheduleDate = DateUtils.parseDate(dateStr);
	    cd90EngineService.autoCd90Schedule(scheduleDate);
	}

}
