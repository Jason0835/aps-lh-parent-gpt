package com.zlt.aps.gsq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈参数信息对外暴露接口（对齐胎圈 ITqParamsService）
 *
 * @author zlt
 * @version 1.0
 * @date 2025-12-12
 */
@FeignClient(contextId = "IGsqParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqParamsService {

    @ApiOperation("查询列表")
    @PostMapping("/gsqParams/list")
    TableDataInfo list(@RequestBody GsqParams queryVO);

    @ApiOperation("保存")
    @PostMapping("/gsqParams/save")
    AjaxResult save(GsqParams gsqParams);

    @ApiOperation("删除")
    @DeleteMapping("/gsqParams/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/gsqParams/{id}")
    GsqParams getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/gsqParams/checkUnique")
    String checkUnique(@RequestBody GsqParams gsqParamsVO);

    @ApiOperation("导出列表")
    @PostMapping("/gsqParams/exportData/{fileName}")
    byte[] exportData(@RequestBody GsqParams queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/gsqParams/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}