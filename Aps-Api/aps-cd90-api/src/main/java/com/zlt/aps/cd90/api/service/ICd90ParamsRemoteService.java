package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICd90ParamsRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90ParamsRemoteService {
    @ApiOperation("查询列表")
    @PostMapping("/cd90Params/list")
    TableDataInfo list(@RequestBody Cd90Params queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd90Params/getInfo/{id}")
    Cd90Params getInfo(@PathVariable("id") Long id);

    @ApiOperation("获取参数值")
    @GetMapping("/cd90Params/getParamValue/{factoryCode}/{paramCode}")
    AjaxResult getParamValue(@PathVariable("factoryCode") String factoryCode, @PathVariable("paramCode") String paramCode);

    @ApiOperation("新增")
    @PostMapping("/cd90Params/add")
    AjaxResult add(@RequestBody Cd90Params entity);

    @ApiOperation("编辑")
    @PostMapping("/cd90Params/edit")
    AjaxResult edit(@RequestBody Cd90Params entity);

    @ApiOperation("删除")
    @PostMapping("/cd90Params/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验唯一性")
    @PostMapping("/cd90Params/checkUnique")
    String checkUnique(@RequestBody Cd90Params entity);

    @ApiOperation("导出")
    @PostMapping("/cd90Params/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90Params queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入")
    @PostMapping("/cd90Params/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}