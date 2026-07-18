//package com.zlt.aps.cd15.engine;
//
//import static com.alibaba.fastjson.JSON.toJSONString;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//import org.junit.FixMethodOrder;
//import org.junit.jupiter.api.Test;
//import org.junit.runners.MethodSorters;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import com.ruoyi.common.core.utils.DateUtils;
//import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
//import com.zlt.aps.cd15.engine.service.Cd15EngineService;
//
//import lombok.extern.slf4j.Slf4j;
//
//@SpringBootTest
//@Slf4j
//@FixMethodOrder(MethodSorters.JVM)
//class ApsEngineCd15ApplicationTests {
//	@Autowired
//	private Cd15EngineService cd15EngineService;
//	@Test
//	public void test() {
//	}
//
////	@Test
//	public void testRun() {
//		String dateStr = "2021-07-10";
//		Date scheduleDate = DateUtils.parseDate(dateStr);
//		cd15EngineService.autoCd15Schedule(scheduleDate);
//	}
//
////	@Test
//	public void testInsert() {
//		String dateStr = "2021-09-01";
//		Date scheduleDate = DateUtils.parseDate(dateStr);
//		Cd15ScheduleResult scheduleResult = new Cd15ScheduleResult();
//		scheduleResult.setScheduleDate(scheduleDate);
//		scheduleResult.setSteelStripCode("HPP0202");
//		scheduleResult.setDayPlanQty(310D);
//		scheduleResult.setNightPlanQty(0D);
//		scheduleResult.setMachineId("247");
//		scheduleResult.setDayHandAnalysis("aaaa");
//		scheduleResult.setNightHandAnalysis("bbbbbb");
//		scheduleResult.setRemark("CCCCCCCC");
//		scheduleResult.setBaseVale(null);
//		cd15EngineService.insertCd15Order(scheduleResult);
//	}
//
////	@Test
//	public void testBatchSave() {
//		String dateStr = "2021-06-28";
//		Date scheduleDate = DateUtils.parseDate(dateStr);
//
//		List<Cd15ScheduleResult> list = new ArrayList<>();
//
//		Cd15ScheduleResult scheduleResult = new Cd15ScheduleResult();
//		scheduleResult.setScheduleDate(scheduleDate);
//		scheduleResult.setBigRollCode("HJZ202");
//		scheduleResult.setSteelStripCode("HPP0202");
//		scheduleResult.setDayPlanQty(500D);
//		scheduleResult.setNightPlanQty(0D);
//		scheduleResult.setMachineId("247");
//
//		Cd15ScheduleResult scheduleResult2 = new Cd15ScheduleResult();
//		scheduleResult2.setScheduleDate(scheduleDate);
//		scheduleResult2.setBigRollCode("HJZ202");
//		scheduleResult2.setSteelStripCode("HPP0206");
//		scheduleResult2.setDayPlanQty(350D);
//		scheduleResult2.setNightPlanQty(0D);
//		scheduleResult2.setMachineId("248");
//
//		list.add(scheduleResult);
//		list.add(scheduleResult2);
//
//		cd15EngineService.batchSaveCd15Schedule(scheduleDate, list);
//	}
//
////	@Test
//	public void testChange() {
//		Cd15ScheduleResult scheduleResult = new Cd15ScheduleResult();
//		scheduleResult.setCxBatchNo("CX20210622001");
//		scheduleResult.setBatchNo("CD1520210628030");
//		scheduleResult.setOrderNo("CD15202106280300002");
//		scheduleResult.setBigRollCode("HSJZ111");
//		scheduleResult.setSteelStripCode("HSP0725");
//		scheduleResult.setDayPlanQty(2067.113D);
//		scheduleResult.setNightPlanQty(0D);
//		scheduleResult.setMachineId("249");
//
//		cd15EngineService.changeCd15Machine("1587", scheduleResult);
//	}
//}
