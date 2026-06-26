package com.zlt.aps.gdyy.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带压延库存 Feign 接口。
 */
@FeignClient(contextId = "IGdyyStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:gdyy}")
public interface IGdyyStockRemoteService {

    @ApiOperation("查询钢带压延库存列表")
    @PostMapping("/gdyy/stock/list")
    TableDataInfo list(@RequestBody GdyyStock queryVO);

    @ApiOperation("获取钢带压延库存详情")
    @GetMapping("/gdyy/stock/getInfo/{id}")
    GdyyStock getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增钢带压延库存")
    @PostMapping("/gdyy/stock/add")
    AjaxResult add(@RequestBody GdyyStock entity);

    @ApiOperation("编辑钢带压延库存")
    @PostMapping("/gdyy/stock/edit")
    AjaxResult edit(@RequestBody GdyyStock entity);

    @ApiOperation("删除钢带压延库存")
    @PostMapping("/gdyy/stock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验钢带压延库存唯一性")
    @PostMapping("/gdyy/stock/checkUnique")
    String checkUnique(@RequestBody GdyyStock entity);

    @ApiOperation("导出钢带压延库存")
    @PostMapping("/gdyy/stock/exportData/{fileName}")
    byte[] exportData(@RequestBody GdyyStock queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入钢带压延库存")
    @PostMapping("/gdyy/stock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
