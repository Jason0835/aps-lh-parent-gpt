package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqAssistSchedule;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 胎圈外协排程结果Service接口
 * @author chen
 * @date 2022-02-16
 */
@FeignClient(contextId = "ITqAssistScheduleService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqAssistScheduleService {

    /**
     * 查询胎圈外协排程结果列表
     */
    @ApiOperation("查询胎圈外协排程结果列表")
    @PostMapping("/assistSchedule/list")
    TableDataInfo list(@RequestBody TqAssistSchedule tqAssistSchedule);

    /**
     * 导出胎圈外协排程结果信息
     */
    @PostMapping("/assistSchedule/export")
    @ApiOperation("导出胎圈外协排程结果信息")
    public byte[] export(@RequestBody TqAssistSchedule assistSchedule);
}
