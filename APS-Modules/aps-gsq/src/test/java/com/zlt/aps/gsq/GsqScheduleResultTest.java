package com.zlt.aps.gsq;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zlt.aps.gsq.engine.service.GsqEngineService;

@SpringBootTest
class GsqScheduleResultTest {
	@Autowired
	private GsqEngineService gsqEngineService;
	
	@Test
	public void test() throws IOException {
	    gsqEngineService.autoGsqSchedule("2025-06-30");
	}

}
