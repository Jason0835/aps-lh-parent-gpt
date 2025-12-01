package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.TmAssistScheduleDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 胎面外协排程结果Service接口
 * @author chen
 * @date 2022-02-15
 */
@FeignClient(contextId = "ITmAssistScheduleService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:tm}")
public interface ITmAssistScheduleService {

    /**
     * 查询胎面外协排程结果列表
     */
    @ApiOperation("查询胎面外协排程结果列表")
    @PostMapping("/tm/assistSchedule/list")
    TableDataInfo list(@RequestBody TmAssistScheduleDto tmAssistSchedule);

    /**
     * 导出列表
     */
    @PostMapping("/tm/assistSchedule/export")
    byte[] export(@RequestBody TmAssistScheduleDto scheduleResult);
}
