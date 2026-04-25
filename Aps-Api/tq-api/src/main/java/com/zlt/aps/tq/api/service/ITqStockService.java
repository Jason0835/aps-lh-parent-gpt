package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "iTqStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqStockService {

    @PostMapping("/tqStock/list")
    TableDataInfo list(@RequestBody TqStock stock);

    @PostMapping("/tqStock/save")
    AjaxResult save(@Validated @RequestBody TqStock stock);

    @PostMapping("/tqStock/delete/{ids}")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @GetMapping("/tqStock/{id}")
    TqStock getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqStock/checkUnique")
    String checkUnique(@Validated @RequestBody TqStock stock);

    @PostMapping("/tqStock/exportData/{fileName}")
    byte[] exportData(@RequestBody TqStock stock, @PathVariable("fileName") String fileName);

    @PostMapping("/tqStock/importData")
    @ApiOperation("导入胎圈库存信息")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
