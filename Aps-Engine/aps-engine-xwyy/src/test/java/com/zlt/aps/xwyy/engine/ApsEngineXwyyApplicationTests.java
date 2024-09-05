package com.zlt.aps.xwyy.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zlt.aps.xwyy.engine.service.impl.XwyyEnginePlanQtyServiceImpl;
import com.zlt.aps.xwyy.engine.vo.XwyyScheduleResultVo;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.engine.service.XwyyEngineService;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@FixMethodOrder(MethodSorters.JVM)
class ApsEngineXwyyApplicationTests {
	@Autowired
	private XwyyEngineService xwyyEngineService;

//    @Test
	public void test() {
	}

	@Test
	public void testRun() {
		String dateStr = "2022-01-06";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		xwyyEngineService.autoXwyySchedule(scheduleDate);
	}

//	@Test
	public void testInsertOrder() {
		String dateStr = "2021-06-28";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		XwyyScheduleResultDto scheduleResult = new XwyyScheduleResultDto();
		scheduleResult.setScheduleDate(scheduleDate);
		scheduleResult.setBigRollCode("HASTLL");
		scheduleResult.setDayPlanQty(110D);
		scheduleResult.setFac2Class1Plan(133D);
		scheduleResult.setFac5Class3Plan(155D);
		scheduleResult.setMachineId("256");
		xwyyEngineService.insertXwyyOrder(scheduleResult);
	}

//	@Test
	public void testBatchSave() {
		String dateStr = "2021-06-28";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		XwyyScheduleResultDto scheduleResult = new XwyyScheduleResultDto();
		scheduleResult.setScheduleDate(scheduleDate);
		scheduleResult.setBigRollCode("HASTLL");
		scheduleResult.setDayPlanQty(110D);
		scheduleResult.setFac2Class1Plan(133D);
		scheduleResult.setFac5Class3Plan(155D);
		scheduleResult.setMachineId("256");

		XwyyScheduleResultDto scheduleResult2 = new XwyyScheduleResultDto();
		scheduleResult2.setScheduleDate(scheduleDate);
		scheduleResult2.setBigRollCode("HSJZ111");
		scheduleResult2.setDayPlanQty(110D);
		scheduleResult2.setFac2Class1Plan(21D);
		scheduleResult2.setFac2Class2Plan(23D);
		scheduleResult2.setFac5Class2Plan(211D);
		scheduleResult2.setFac5Class3Plan(78D);
		scheduleResult2.setMachineId("1606");

		List<XwyyScheduleResultDto> list = new ArrayList<>();
		list.add(scheduleResult);
		list.add(scheduleResult2);
		xwyyEngineService.batchSaveXwyySchedule(scheduleDate, list);
	}
	
//	@Test
	public void testChange() {
		String dateStr = "2021-06-28";
		Date scheduleDate = DateUtils.parseDate(dateStr);
		XwyyScheduleResultDto scheduleResult = new XwyyScheduleResultDto();
		scheduleResult.setScheduleDate(scheduleDate);
		scheduleResult.setBatchNo("XWYY20210628030");
		scheduleResult.setOrderNo("XWYY202106280300003");
		scheduleResult.setBigRollCode("HSJZ303");
		scheduleResult.setMachineId("1640");
		scheduleResult.setDayPlanQty(3120.960);
		
		xwyyEngineService.changeXwyyMachine("1644", scheduleResult);
	}

	public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		XwyyEnginePlanQtyServiceImpl xwyyEnginePlanQtyService = new XwyyEnginePlanQtyServiceImpl();
		List<XwyyScheduleResultVo> scheduleList = new ArrayList<>();
		BigDecimal remainder = BigDecimal.valueOf(5);
		// 中班卷,夜班卷,可供应时长
		double[][] a = {
				{4, 6, 1},
				// {2, 0, 1},
				// {0, 3, 3},
				// {0, 3, 3},
				// {0, 2, 3},
		};
		int n=a.length;
		while (n-- > 0) {
			XwyyScheduleResultVo xwyyScheduleResultVo = new XwyyScheduleResultVo();
			// 标准
			xwyyScheduleResultVo.setRollStandardSize(BigDecimal.valueOf(400));
			// 中班
			xwyyScheduleResultVo.setDayPlanQtyNum(a[n][0]);
			xwyyScheduleResultVo.setDayPlanQty(xwyyScheduleResultVo.getRollStandardSize().multiply(BigDecimal.valueOf(xwyyScheduleResultVo.getDayPlanQtyNum())).doubleValue());
			// 夜班
			xwyyScheduleResultVo.setNightPlanQtyNum(a[n][1]);
			xwyyScheduleResultVo.setNightPlanQty(xwyyScheduleResultVo.getRollStandardSize().multiply(BigDecimal.valueOf(xwyyScheduleResultVo.getNightPlanQtyNum())).doubleValue());
			// 可供应时长
			xwyyScheduleResultVo.setSupplyTime(a[n][2]);
			scheduleList.add(xwyyScheduleResultVo);
		}
		showData(scheduleList);
		Method originalLineEquilibrium = xwyyEnginePlanQtyService.getClass().getDeclaredMethod("originalLineEquilibrium", List.class,BigDecimal.class);
		originalLineEquilibrium.setAccessible(true);
		originalLineEquilibrium.invoke(xwyyEnginePlanQtyService,scheduleList, remainder);
		showData(scheduleList);
	}

	static void showData(List<XwyyScheduleResultVo> scheduleList) {
		int n = scheduleList.size();
		while (n-- > 0) {
			XwyyScheduleResultVo v = scheduleList.get(n);
			System.out.println(v.getDayPlanQtyNum() + "   :   " + v.getNightPlanQtyNum() + "   ||   " + v.getDayPlanQty() + "   :   " + v.getNightPlanQty());
		}
		System.out.println("====");
	}
}
