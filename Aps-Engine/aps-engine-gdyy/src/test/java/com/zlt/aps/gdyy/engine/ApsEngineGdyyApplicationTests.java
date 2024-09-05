package com.zlt.aps.gdyy.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;
import com.zlt.aps.gdyy.engine.service.GdyyEngineService;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
class ApsEngineGdyyApplicationTests {
	@Autowired
	private GdyyEngineService gdyyEngineService;

	@Test
	public void test() {
	}

//    @Test
	public void testRun() {
		String dateStr = "2021-08-21";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		gdyyEngineService.autoGdyySchedule(scheduleDate);
	}

//    @Test
	public void testInsert() {
		String dateStr = "2021-06-28";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		GdyyScheduleResultDto scheduleResult = new GdyyScheduleResultDto();
		scheduleResult.setScheduleDate(scheduleDate);
		scheduleResult.setBigRollCode("HASTSR");
		scheduleResult.setDayUsed(3D);
		scheduleResult.setStockQty(33D);
		scheduleResult.setClass1Plan(111D);
		scheduleResult.setClass1Finish(11D);
		scheduleResult.setClass1Remark("1");
		scheduleResult.setClass2Plan(222D);
		scheduleResult.setClass2Finish(22D);
		scheduleResult.setClass2Remark("2");
		scheduleResult.setClass3Plan(333D);
		scheduleResult.setClass3Finish(33D);
		scheduleResult.setClass3Remark("3");
		scheduleResult.setRemark("aaaa");
		gdyyEngineService.insertGdyyOrder(scheduleResult);
	}

//    @Test
	public void testBatchSave() {
		String dateStr = "2021-06-28";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		GdyyScheduleResultDto scheduleResult = new GdyyScheduleResultDto();
		scheduleResult.setScheduleDate(scheduleDate);
		scheduleResult.setBigRollCode("HASTJT");
		scheduleResult.setDayUsed(1D);
		scheduleResult.setStockQty(33D);
		scheduleResult.setClass1Plan(1D);
		scheduleResult.setClass1Finish(110D);
		scheduleResult.setClass1Remark("111");
		scheduleResult.setClass2Plan(2D);
		scheduleResult.setClass2Finish(220D);
		scheduleResult.setClass2Remark("222");
		scheduleResult.setClass3Plan(3D);
		scheduleResult.setClass3Finish(330D);
		scheduleResult.setClass3Remark("333");
		scheduleResult.setRemark("aaaa01");

		GdyyScheduleResultDto scheduleResult2 = new GdyyScheduleResultDto();
		scheduleResult2.setScheduleDate(scheduleDate);
		scheduleResult2.setBigRollCode("HASTLL");
		scheduleResult2.setDayUsed(5D);
		scheduleResult2.setStockQty(99D);
		scheduleResult2.setClass1Plan(111D);
		scheduleResult2.setClass1Finish(11D);
		scheduleResult2.setClass1Remark("1");
		scheduleResult2.setClass2Plan(222D);
		scheduleResult2.setClass2Finish(22D);
		scheduleResult2.setClass2Remark("2");
		scheduleResult2.setClass3Plan(333D);
		scheduleResult2.setClass3Finish(33D);
		scheduleResult2.setClass3Remark("3");
		scheduleResult2.setRemark("aaaa02");

		List<GdyyScheduleResultDto> list = new ArrayList<>();
		list.add(scheduleResult);
		list.add(scheduleResult2);
		gdyyEngineService.batchSaveGdyySchedule(scheduleDate, list);
	}
}
