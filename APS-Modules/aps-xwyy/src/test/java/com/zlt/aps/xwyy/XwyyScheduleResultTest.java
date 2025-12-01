package com.zlt.aps.xwyy;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.xwyy.engine.service.XwyyEngineService;

@SpringBootTest
class XwyyScheduleResultTest {
	@Autowired
	private XwyyEngineService xwyyEngineService;
	
	@Test
	public void test() throws IOException {
        String dateStr = "2025-07-10";
        Date scheduleDate = DateUtils.parseDate(dateStr);
	    xwyyEngineService.autoXwyySchedule(scheduleDate);
	}

}
