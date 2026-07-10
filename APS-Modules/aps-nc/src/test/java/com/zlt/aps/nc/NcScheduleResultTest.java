package com.zlt.aps.nc;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zlt.aps.nc.api.domain.entity.NcStock;
import com.zlt.aps.nc.controller.NcStockController;
import com.zlt.aps.nc.engine.service.NcEngineService;

@SpringBootTest
class NcScheduleResultTest {
	@Autowired
	private NcEngineService ncEngineService;
    @Autowired
	private NcStockController ncStockController;
	
	@Test
	public void test() throws IOException {
//	    ncEngineService.autoNcSchedule("2025-08-13");
//	    NcStock stock = new NcStock();
//	    stock.setFactoryCode("116");
//	    ncStockController.list(stock);
	}

}
