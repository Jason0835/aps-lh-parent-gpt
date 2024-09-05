package com.zlt.aps.cd15.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.service.Cd15EngineService;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@FixMethodOrder(MethodSorters.JVM)
class ApsEngineCd15ApplicationTests {

	@Resource(name = "cd15EngineService")
	private Cd15EngineService cd15EngineService;
	@Autowired
	private CxEngineQuotaCommonService cxEngineQuotaCommonService;

	@Test
	public void test() {
//		cxEngineQuotaCommonService.getCxMachineQuota(new String[] {"E05$YHETB6861"});
	    Pattern pattern= Pattern.compile("\\b(and|exec|insert|select|drop|grant|alter|delete|update|count|chr|mid|master|truncate|char|declare|or)\\b|(\\*|;|\\+|'|%|--)");
	    Matcher matcher=pattern.matcher("--".toLowerCase()); 
	    log.info(String.valueOf(matcher.find()));
	}

	/**
	 * 自动排程
	 */
    @Test
	public void testRun() {
		String dateStr = "2022-02-10";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		cd15EngineService.autoCd15Schedule(scheduleDate);
	}

	/**
	 * 插单
	 */
//    @Test
	public void testInsert() {
		String dateStr = "2021-06-28";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		Cd15ScheduleResult scheduleResult = new Cd15ScheduleResult();
		scheduleResult.setScheduleDate(scheduleDate);
		scheduleResult.setSteelStripCode1("HK2971");
		scheduleResult.setSteelStripCode2("HU3062");
		scheduleResult.setDayPlanQty1(312D);
		scheduleResult.setNightPlanQty1(0D);
		scheduleResult.setDayHandAnalysis1("xxxx");
		scheduleResult.setNightHandAnalysis1("yyyy");
		scheduleResult.setMachineId("456");
		cd15EngineService.insertCd15Order(scheduleResult);
	}

	/**
	 * 批量导入
	 */
//    @Test
	public void testBatchSave() {
		String dateStr = "2021-06-28";
		Date scheduleDate = DateUtils.parseDate(dateStr);

		List<Cd15ScheduleResult> list = new ArrayList<>();
		
		Cd15ScheduleResult scheduleResult = new Cd15ScheduleResult();
		scheduleResult.setScheduleDate(scheduleDate);
		scheduleResult.setBigRollCode("HASTSR");
		scheduleResult.setSteelStripCode1("HK2971");
		scheduleResult.setSteelStripCode2("HU3062");
		scheduleResult.setCuttingAngle(23D);
		scheduleResult.setDayPlanQty1(312D);
//		scheduleResult.setNightPlanQty1(0D);
		scheduleResult.setDayHandAnalysis1("aaaa");
		scheduleResult.setNightHandAnalysis1("bbb");
		scheduleResult.setMachineId("456");
		
		Cd15ScheduleResult scheduleResult2 = new Cd15ScheduleResult();
		scheduleResult2.setScheduleDate(scheduleDate);
		scheduleResult2.setBigRollCode("HASTLL");
		scheduleResult2.setSteelStripCode1("HK2224");
		scheduleResult2.setSteelStripCode2("HU2325");
		scheduleResult2.setCuttingAngle(25D);
		scheduleResult2.setDayPlanQty1(220D);
		scheduleResult2.setNightPlanQty1(0D);
		scheduleResult2.setDayHandAnalysis1("cccc");
		scheduleResult2.setNightHandAnalysis1("dddd");
		scheduleResult2.setMachineId("455");
		
		list.add(scheduleResult);
		list.add(scheduleResult2);
		
		cd15EngineService.batchSaveCd15Schedule(scheduleDate, list);
	}


	/**
	 * 转机台
	 */
//	@Test
	public void testChangeMaince( ) {
		Cd15ScheduleResult scheduleResult = new Cd15ScheduleResult();
		scheduleResult.setBatchNo("CX20210622001");
		scheduleResult.setOrderNo("CD1520210628002");
		scheduleResult.setBigRollCode("HASTLA");
		scheduleResult.setSteelStripCode1("HK3801");
		scheduleResult.setSteelStripCode2("HU3525");
		scheduleResult.setCuttingAngle(31D);
		scheduleResult.setDayPlanQty1(477.932D);
		scheduleResult.setNightPlanQty1(0D);
		scheduleResult.setMachineId("456");

		cd15EngineService.changeCd15Machine("246", scheduleResult);
	}

}
