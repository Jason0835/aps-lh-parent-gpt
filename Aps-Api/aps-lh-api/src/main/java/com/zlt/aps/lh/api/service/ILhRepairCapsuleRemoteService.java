package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.aps.mdm.api.domain.entity.MdmLhRepairCapsule;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "ILhRepairCapsuleRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhRepairCapsuleRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/lhRepairCapsule/list")
    TableDataInfo list(@RequestBody MdmLhRepairCapsule query);

    @ApiOperation("获取详细信息")
    @GetMapping("/lhRepairCapsule/{id}")
    MdmLhRepairCapsule getInfo(@PathVariable("id") Long id);
}
