package com.zlt.aps.gdyy;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.gdyy.engine.service.GdyyEngineService;

@SpringBootTest
class GdyyScheduleResultTest {
	@Autowired
	private GdyyEngineService gdyyEngineService;
	
	@Test
	public void test() throws IOException {
        String dateStr = "2025-07-23";
        Date scheduleDate = DateUtils.parseDate(dateStr);
	    gdyyEngineService.autoGdyySchedule(scheduleDate);
	}

}
