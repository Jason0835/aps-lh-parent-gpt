package com.zlt.aps.job.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 定时任务-月计划相关
 *
 * @author Liam
 * @since 2025/4/10
 */
@FeignClient(contextId = "IMonthPlanTaskService", name = "${remoteApi.value.monthplan:aps-monthplan}")
public interface IMonthPlanTaskService {

    /**
     * 内销历史销售订单同步
     *
     * @return 结果
     */
    @ApiOperation("内销历史销售订单同步")
    @PostMapping("/saleOrderSync/syncInHisSaleOrder")
    public AjaxResult syncInHisSaleOrder();

    /**
     * 同步MES的SAP与施工关系
     *
     * @return 结果
     */
    @ApiOperation("同步MES的SAP与施工关系")
    @PostMapping("/mdmProductConstruction/syncProductConstructionInfo")
    public AjaxResult syncProductConstructionInfo();
}
