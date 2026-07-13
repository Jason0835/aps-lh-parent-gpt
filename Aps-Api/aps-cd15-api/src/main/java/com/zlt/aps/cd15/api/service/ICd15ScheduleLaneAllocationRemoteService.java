package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** CD15斜裁排程库排分配明细远程服务。 */
@FeignClient(contextId = "ICd15ScheduleLaneAllocationRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15ScheduleLaneAllocationRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/cd15ScheduleLaneAllocation/list")
    TableDataInfo list(@RequestBody Cd15ScheduleLaneAllocation queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd15ScheduleLaneAllocation/getInfo/{id}")
    Cd15ScheduleLaneAllocation getInfo(@PathVariable("id") Long id);

    @ApiOperation("导出")
    @PostMapping("/cd15ScheduleLaneAllocation/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15ScheduleLaneAllocation queryVO, @PathVariable("fileName") String fileName);
}