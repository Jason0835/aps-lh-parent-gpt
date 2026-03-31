package com.zlt.aps.itf.mes;

import com.zlt.aps.itf.ApsItfApplication;
import com.zlt.aps.itf.mes.service.MesItfService;
import com.zlt.aps.mp.api.domain.entity.MdmOutbountOrdersNotScan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ApsItfApplication.class)
class MesItfServiceTest {

    @Autowired
    private MesItfService mesItfService;

    @Test
    void testSyncOutbountOrdersNotScan() {
        MdmOutbountOrdersNotScan param = new MdmOutbountOrdersNotScan();
        param.setFactoryCode("1000");
        mesItfService.syncOutbountOrdersNotScan(param);
    }
}
