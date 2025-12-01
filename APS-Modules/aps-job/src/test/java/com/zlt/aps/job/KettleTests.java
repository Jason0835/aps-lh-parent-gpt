package com.zlt.aps.job;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Lazy;

import com.zlt.aps.job.task.RequestMesTask;
import com.zlt.kettle.controller.KettleBizController;

@SpringBootTest
class KettleTests {
	@Autowired
	private KettleBizController kettleBizController;

    @Autowired
	private RequestMesTask requestMesTask;
	
	@Test
	public void test() throws IOException {
	    requestMesTask.runApsSyncData("defaults");
//	    kettleBizController.startTrans("1", 8);
	}

}
