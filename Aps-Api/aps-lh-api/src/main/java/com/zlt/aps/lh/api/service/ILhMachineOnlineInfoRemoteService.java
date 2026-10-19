package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 硫化在机信息前端接口
 *
 * @author APS Team
 * @date 2026-04-17
 */
@FeignClient(contextId = "ILhMachineOnlineInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhMachineOnlineInfoRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/lhMachineOnlineInfo/list")
    TableDataInfo list(@RequestBody LhMachineOnlineInfo queryVO);

    @ApiOperation("根据ID获取详情信息")
    @GetMapping("/lhMachineOnlineInfo/{id}")
    LhMachineOnlineInfo getInfo(@PathVariable("id") Long id);

    @ApiOperation("导出数据")
    @PostMapping("/lhMachineOnlineInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody LhMachineOnlineInfo queryVO, @PathVariable("fileName") String fileName);
}

