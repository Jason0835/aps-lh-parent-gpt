package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcCurlRollRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcCurlRollRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcCurlRoll/list")
    TableDataInfo list(@RequestBody TcCurlRoll queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcCurlRoll/save")
    AjaxResult save(TcCurlRoll tcCurlRoll);

    @ApiOperation("删除")
    @DeleteMapping("/tcCurlRoll/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcCurlRoll/{id}")
    TcCurlRoll getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcCurlRoll/checkUnique")
    String checkUnique(@RequestBody TcCurlRoll tcCurlRollVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcCurlRoll/exportData/{fileName}")
    byte[] exportData(@RequestBody TcCurlRoll queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcCurlRoll/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}