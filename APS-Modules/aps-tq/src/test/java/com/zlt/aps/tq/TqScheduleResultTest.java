package com.zlt.aps.tq;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zlt.aps.tq.engine.service.TqEngineService;

@SpringBootTest
class TqScheduleResultTest {
	@Autowired
	private TqEngineService tqEngineService;
	
	@Test
	public void test() throws IOException {
	    tqEngineService.autoTqSchedule("2025-07-05");
	}

}
