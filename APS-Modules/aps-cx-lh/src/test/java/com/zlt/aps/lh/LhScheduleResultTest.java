package com.zlt.aps.lh;

import java.io.IOException;
import java.util.Date;

import javax.annotation.Resource;

//import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.cx.controller.CxScheduleResultController;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.controller.LhScheduleResultController;
import com.zlt.aps.lh.handle.LhSyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;

//@SpringBootTest
class LhScheduleResultTest {
	@Autowired
	private LhScheduleResultController lhScheduleResultController;
    @Autowired
    private CxScheduleResultController cxScheduleResultController;
    @Resource
    private LhSyncDataHandle syncDataHandle;
	
//	@Test
	public void test() throws IOException {
	    Long[] ids = new Long[] {143522L};
	    LhScheduleResult dto = new LhScheduleResult();
	    dto.setIds(ids);
        String dateStr = "2025-08-22";
        Date scheduleDate = DateUtils.parseDate(dateStr);
        dto.setScheduleDate(scheduleDate);
//        dto.setId(142539L);
	    lhScheduleResultController.publish(dto);
	}
	

//    @Test
    public void test1() throws IOException {
        SyncParamsVO syncParamsVO = new SyncParamsVO();
        syncParamsVO.setSyncKey("MIX_GLUE_SCHE_FBK");
        syncParamsVO.setDataVersion("APS_MES_AH01_2025082100084");
        syncParamsVO.setFactoryCode("AH01");
        syncParamsVO.setCompanyCode("AH01");
        syncDataHandle.syncNotice(syncParamsVO);
    }
	
//	@Test
    public void test2() throws IOException {
        Long[] ids = new Long[] {395929L};
        CxScheduleResult dto = new CxScheduleResult();
        dto.setIds(ids);
        String dateStr = "2025-08-19";
        Date scheduleDate = DateUtils.parseDate(dateStr);
        dto.setScheduleDate(scheduleDate);
        cxScheduleResultController.publish(dto);
    }

}
