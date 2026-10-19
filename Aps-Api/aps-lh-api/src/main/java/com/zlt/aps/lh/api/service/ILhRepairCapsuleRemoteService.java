package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        contextId = "ILhRepairCapsuleRemoteService",
        value = ServiceNameConstants.GATEWAY_SERVICE,
        path = "${api.path.lh:/lh}"
)
public interface ILhRepairCapsuleRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/lhRepairCapsule/list")
    TableDataInfo list(@RequestBody LhRepairCapsule query);

    @ApiOperation("获取详细信息")
    @GetMapping("/lhRepairCapsule/{id}")
    LhRepairCapsule getInfo(@PathVariable("id") Long id);

    @ApiOperation("导出数据")
    @PostMapping("/lhRepairCapsule/exportData/{fileName}")
    byte[] exportData(@RequestBody LhRepairCapsule queryVO, @PathVariable("fileName") String fileName);
}

