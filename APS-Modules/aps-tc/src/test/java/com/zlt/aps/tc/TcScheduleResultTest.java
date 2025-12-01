package com.zlt.aps.tc;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zlt.aps.tc.engine.service.TcEngineService;

@SpringBootTest
class TcScheduleResultTest {
	@Autowired
	private TcEngineService tcEngineService;
	
	@Test
	public void test() throws IOException {
	    tcEngineService.autoTcSchedule("2025-08-18");
	}

}
