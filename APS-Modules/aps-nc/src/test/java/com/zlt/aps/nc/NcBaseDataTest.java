package com.zlt.aps.nc;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcGlueOrder;
import com.zlt.aps.nc.api.domain.entity.NcStock;
import com.zlt.aps.nc.controller.NcGlueOrderController;
import com.zlt.aps.nc.controller.NcStockController;

@SpringBootTest
class NcBaseDataTest {
    @Autowired
	private NcStockController ncStockController;
    @Autowired
    private NcGlueOrderController ncGlueOrderController;
	
	@Test
	public void test() throws IOException {
	    NcStock stock = new NcStock();
	    stock.setFactoryCode("116");
//	    ncStockController.list(stock);
	    
	    NcGlueOrder ncGlueOrder = new NcGlueOrder();
	    ncGlueOrder.setFactoryCode("116");
	    TableDataInfo t = ncGlueOrderController.list(ncGlueOrder);
	    t.getRows();
	}

}
