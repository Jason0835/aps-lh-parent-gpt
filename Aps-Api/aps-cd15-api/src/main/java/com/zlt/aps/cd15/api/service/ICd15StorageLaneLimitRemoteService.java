package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 斜裁库排限制 Feign 接口。
 */
@FeignClient(contextId = "ICd15StorageLaneLimitRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15StorageLaneLimitRemoteService {

    /** 查询列表 */
    @ApiOperation("查询斜裁库排限制列表")
    @PostMapping("/cd15StorageLaneLimit/list")
    TableDataInfo list(@RequestBody Cd15StorageLaneLimit queryVO);

    /** 获取详情 */
    @ApiOperation("获取斜裁库排限制详情")
    @GetMapping("/cd15StorageLaneLimit/getInfo/{id}")
    Cd15StorageLaneLimit getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增斜裁库排限制")
    @PostMapping("/cd15StorageLaneLimit/add")
    AjaxResult add(@RequestBody Cd15StorageLaneLimit entity);

    /** 编辑 */
    @ApiOperation("编辑斜裁库排限制")
    @PostMapping("/cd15StorageLaneLimit/edit")
    AjaxResult edit(@RequestBody Cd15StorageLaneLimit entity);

    /** 删除 */
    @ApiOperation("删除斜裁库排限制")
    @PostMapping("/cd15StorageLaneLimit/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 校验唯一性 */
    @ApiOperation("校验斜裁库排限制唯一性")
    @PostMapping("/cd15StorageLaneLimit/checkUnique")
    String checkUnique(@RequestBody Cd15StorageLaneLimit entity);

    /** 导出数据 */
    @ApiOperation("导出斜裁库排限制")
    @PostMapping("/cd15StorageLaneLimit/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15StorageLaneLimit queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入斜裁库排限制")
    @PostMapping("/cd15StorageLaneLimit/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}