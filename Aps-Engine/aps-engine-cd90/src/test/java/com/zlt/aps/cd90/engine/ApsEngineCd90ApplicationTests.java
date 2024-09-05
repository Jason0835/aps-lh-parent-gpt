package com.zlt.aps.cd90.engine;

import static com.alibaba.fastjson.JSON.toJSONString;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.service.Cd90EngineService;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@FixMethodOrder(MethodSorters.JVM)
class ApsEngineCd90ApplicationTests {
	@Autowired
	private Cd90EngineService cd90EngineService;
	@Test
	public void test() {
	}

//	@Test
	public void testRun() {
		String dateStr = "2021-07-10";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		cd90EngineService.autoCd90Schedule(scheduleDate);
	}

//	@Test
	public void testInsert() {
		String dateStr = "2021-09-01";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		Cd90ScheduleResult scheduleResult = new Cd90ScheduleResult();
		scheduleResult.setScheduleDate(scheduleDate);
		scheduleResult.setClothCode("HPP0202");
		scheduleResult.setDayPlanQty(310D);
		scheduleResult.setNightPlanQty(0D);
		scheduleResult.setMachineId("247");
		scheduleResult.setDayHandAnalysis("aaaa");
		scheduleResult.setNightHandAnalysis("bbbbbb");
		scheduleResult.setRemark("CCCCCCCC");
		scheduleResult.setBaseVale(null);
		cd90EngineService.insertCd90Order(scheduleResult);
	}

//	@Test
	public void testBatchSave() {
		String dateStr = "2021-06-28";
		Date scheduleDate = DateUtils.parseDate(dateStr);

		List<Cd90ScheduleResult> list = new ArrayList<>();

		Cd90ScheduleResult scheduleResult = new Cd90ScheduleResult();
		scheduleResult.setScheduleDate(scheduleDate);
		scheduleResult.setBigRollCode("HJZ202");
		scheduleResult.setClothCode("HPP0202");
		scheduleResult.setDayPlanQty(500D);
		scheduleResult.setNightPlanQty(0D);
		scheduleResult.setMachineId("247");

		Cd90ScheduleResult scheduleResult2 = new Cd90ScheduleResult();
		scheduleResult2.setScheduleDate(scheduleDate);
		scheduleResult2.setBigRollCode("HJZ202");
		scheduleResult2.setClothCode("HPP0206");
		scheduleResult2.setDayPlanQty(350D);
		scheduleResult2.setNightPlanQty(0D);
		scheduleResult2.setMachineId("248");

		list.add(scheduleResult);
		list.add(scheduleResult2);

		cd90EngineService.batchSaveCd90Schedule(scheduleDate, list);
	}

//	@Test
	public void testChange() {
		Cd90ScheduleResult scheduleResult = new Cd90ScheduleResult();
		scheduleResult.setCxBatchNo("CX20210622001");
		scheduleResult.setBatchNo("CD9020210628030");
		scheduleResult.setOrderNo("CD90202106280300002");
		scheduleResult.setBigRollCode("HSJZ111");
		scheduleResult.setClothCode("HSP0725");
		scheduleResult.setDayPlanQty(2067.113D);
		scheduleResult.setNightPlanQty(0D);
		scheduleResult.setMachineId("249");

		cd90EngineService.changeCd90Machine("1587", scheduleResult);
	}
}
