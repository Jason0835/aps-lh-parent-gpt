package com.zlt.aps.job.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.itf.mes.IMesHalfPartsItfService;

import io.swagger.annotations.ApiOperation;

/**
 * MES接口定时任务
 *
 * @author zlt
 * @since 2026/07/31
 */
@Component("mesHalfParts")
public class MesHalfPartsTask {

    @Autowired
    private IMesHalfPartsItfService iMesHalfPartsItfService;

    /**
     * 同步垫胶库存
     */
    @ApiOperation("同步成品库存-默认当前年月")
    public void syncDjStock() {
        FeignTokenHelper.runWithToken(() -> iMesHalfPartsItfService.syncDjStock());
    }

    /**
     * 同步内衬库存
     */
    @ApiOperation("同步成品库存-默认当前年月")
    public void syncNcStock() {
        FeignTokenHelper.runWithToken(() -> iMesHalfPartsItfService.syncNcStock());
    }
}
