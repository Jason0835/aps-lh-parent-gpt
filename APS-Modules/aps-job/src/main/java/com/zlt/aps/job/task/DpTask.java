package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.MpMonthlySaleQty;
import com.zlt.aps.mp.api.service.IMpMonthlySaleQtyRemoteService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 需求计划相关定时任务
 *
 * @author Chen
 * @since 2025/12/23
 */
@Component("dpTask")
@Slf4j
public class DpTask {

    @Autowired
    private IMpMonthlySaleQtyRemoteService iMpMonthlySaleQtyRemoteService;

    /**
     * 定时生成月均销量
     */
    @ApiOperation("定时生成月均销量")
    public void genMonthlySaleQty() {
        MpMonthlySaleQty mpMonthlySaleQty = new MpMonthlySaleQty();
        mpMonthlySaleQty.setFactoryCode("116");
        AjaxResult ajaxResult = iMpMonthlySaleQtyRemoteService.genMonthlySaleQty(mpMonthlySaleQty);
        log.info(ajaxResult.toString());
    }

}
