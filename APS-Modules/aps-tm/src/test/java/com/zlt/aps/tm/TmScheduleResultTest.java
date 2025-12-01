package com.zlt.aps.tm;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zlt.aps.tm.engine.service.TmEngineService;

@SpringBootTest
class TmScheduleResultTest {
	@Autowired
	private TmEngineService tmEngineService;
	
	@Test
	public void test() throws IOException {
	    tmEngineService.autoTmSchedule("2025-08-18");
	}

}
