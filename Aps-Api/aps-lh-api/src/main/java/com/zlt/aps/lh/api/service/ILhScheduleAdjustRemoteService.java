package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhCxLinkageConfirm;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 硫化排程调整RemoteService
 */
@FeignClient(contextId = "ILhScheduleAdjustRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhScheduleAdjustRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhScheduleAdjust/list")
    TableDataInfo list(@RequestBody LhCxLinkageConfirm entity);

    /**
    * 硫化成型联动确认
    */
    @ApiOperation("硫化成型联动确认")
    @PostMapping("/lhScheduleAdjust/confirmAdjust")
    AjaxResult confirmAdjust(@RequestBody LhCxLinkageConfirm entity);
}
