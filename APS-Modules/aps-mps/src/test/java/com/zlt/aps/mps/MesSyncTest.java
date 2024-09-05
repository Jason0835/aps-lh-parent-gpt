package com.zlt.aps.mps;

import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.mps.controller.MesMergeController;
import com.zlt.aps.mps.domain.TCxClassShiftFinishQty;
import com.zlt.aps.mps.mapper.TCxClassShiftFinishQtyMapper;
import com.zlt.aps.mps.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author Gim
 */
@SpringBootTest
public class MesSyncTest {

	@Autowired
	private MesConstructionInfoService mesConstructionInfoService;

	@Autowired
	private MonthPlanStatisticsService monthPlanStatisticsService;

	@Autowired
	private MesMergeController mergeController;
	@Resource
	private TCxClassShiftFinishQtyMapper cxClassShiftFinishQtyMapper;
	
	@Test
    public void test() {
//		mesConstructionInfoService.mergeConstructionInfo();
		monthPlanStatisticsService.actualOverProduction();
    }

    @Test
	public void mpsToFacTest() {
		mergeController.mpsSyncTest("APS202109070001", "MPS_TO_APS_FAC", "2021", "09", "0");
	}

	@Test
	public void getCxClassTest() {
		Date date = DateUtil.from("2021-09");
		String dates = DateUtil.formatMonth(date);
		List<TCxClassShiftFinishQty> list = cxClassShiftFinishQtyMapper.selectCxByScheduleDate(dates);
		for (TCxClassShiftFinishQty qty : list) {
			System.out.println("qty = " + qty);
		}
	}
}
