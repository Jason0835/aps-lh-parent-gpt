package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/** 直裁未排结果远程服务。 */
@FeignClient(contextId = "ICd90UnscheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90UnscheduleResultRemoteService {
    @ApiOperation("查询列表")
    @PostMapping("/cd90UnscheduleResult/list")
    TableDataInfo list(@RequestBody Cd90UnscheduleResult queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd90UnscheduleResult/getInfo/{id}")
    Cd90UnscheduleResult getInfo(@PathVariable("id") Long id);

    @ApiOperation("导出")
    @PostMapping("/cd90UnscheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90UnscheduleResult queryVO, @PathVariable("fileName") String fileName);
}
