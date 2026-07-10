package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 斜裁库存管理 Feign 接口。
 */
@FeignClient(contextId = "ICd15StockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15StockRemoteService {

    /** 查询列表 */
    @ApiOperation("查询斜裁库存列表")
    @PostMapping("/cd15Stock/list")
    TableDataInfo list(@RequestBody Cd15Stock queryVO);

    /** 获取详情 */
    @ApiOperation("获取斜裁库存详情")
    @GetMapping("/cd15Stock/getInfo/{id}")
    Cd15Stock getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增斜裁库存")
    @PostMapping("/cd15Stock/add")
    AjaxResult add(@RequestBody Cd15Stock entity);

    /** 编辑 */
    @ApiOperation("编辑斜裁库存")
    @PostMapping("/cd15Stock/edit")
    AjaxResult edit(@RequestBody Cd15Stock entity);

    /** 删除 */
    @ApiOperation("删除斜裁库存")
    @PostMapping("/cd15Stock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 校验同工厂、日期、物料唯一 */
    @ApiOperation("校验斜裁库存唯一性")
    @PostMapping("/cd15Stock/checkUnique")
    String checkUnique(@RequestBody Cd15Stock entity);

    /** 导出数据 */
    @ApiOperation("导出斜裁库存")
    @PostMapping("/cd15Stock/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15Stock queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入斜裁库存")
    @PostMapping("/cd15Stock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
