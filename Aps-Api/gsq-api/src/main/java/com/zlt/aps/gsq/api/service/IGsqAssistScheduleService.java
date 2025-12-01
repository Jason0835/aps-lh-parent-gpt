package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqAssistSchedule;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 钢丝圈外协排程结果Service接口
 * @author chen
 * @date 2022-02-15
 */
@FeignClient(contextId = "IGsqAssistScheduleService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqAssistScheduleService {

    /**
     * 查询钢丝圈外协排程结果列表
     */
    @ApiOperation("查询钢丝圈外协排程结果列表")
    @PostMapping("/gsq/assistSchedule/list")
    TableDataInfo list(@RequestBody GsqAssistSchedule gsqAssistSchedule);

    /**
     * 导出列表
     */
    @PostMapping("/gsq/assistSchedule/export")
    byte[] export(@RequestBody GsqAssistSchedule assistSchedule);
}
