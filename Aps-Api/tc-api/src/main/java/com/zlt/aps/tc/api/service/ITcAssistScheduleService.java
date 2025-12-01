package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcAssistSchedule;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 胎侧外协排程结果Service接口
 * @author chen
 * @date 2022-02-15
 */
@FeignClient(contextId = "ITcAssistScheduleService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcAssistScheduleService {

    /**
     * 查询胎侧外协排程结果列表
     */
    @ApiOperation("查询胎侧外协排程结果列表")
    @PostMapping("/tc/assistSchedule/list")
    TableDataInfo list(@RequestBody TcAssistSchedule tcAssistSchedule);

    /**
     * 导出列表
     */
    @PostMapping("/tc/assistSchedule/export")
    byte[] export(@RequestBody TcAssistSchedule scheduleResult);
}
