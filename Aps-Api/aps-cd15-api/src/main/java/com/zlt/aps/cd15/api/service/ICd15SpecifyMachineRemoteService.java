package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 斜裁定点机台 Feign 接口。
 */
@FeignClient(contextId = "ICd15SpecifyMachineRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15SpecifyMachineRemoteService {

    /** 查询列表 */
    @ApiOperation("查询斜裁定点机台列表")
    @PostMapping("/specifyMachine/list")
    TableDataInfo list(@RequestBody Cd15SpecifyMachine queryVO);

    /** 获取详情 */
    @ApiOperation("获取斜裁定点机台详情")
    @GetMapping("/specifyMachine/getInfo/{id}")
    Cd15SpecifyMachine getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增斜裁定点机台")
    @PostMapping("/specifyMachine/add")
    AjaxResult add(@RequestBody Cd15SpecifyMachine specifyMachine);

    /** 编辑 */
    @ApiOperation("编辑斜裁定点机台")
    @PostMapping("/specifyMachine/edit")
    AjaxResult edit(@RequestBody Cd15SpecifyMachine specifyMachine);

    /** 删除 */
    @ApiOperation("删除斜裁定点机台")
    @PostMapping("/specifyMachine/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 清空 */
    @ApiOperation("清空斜裁定点机台")
    @PostMapping("/specifyMachine/removeAll")
    AjaxResult removeAll(@RequestBody Cd15SpecifyMachine queryVO);

    /** 校验唯一性 */
    @ApiOperation("校验斜裁定点机台唯一性")
    @PostMapping("/specifyMachine/checkUnique")
    String checkUnique(@RequestBody Cd15SpecifyMachine specifyMachine);

    /** 导出数据 */
    @ApiOperation("导出斜裁定点机台")
    @PostMapping("/specifyMachine/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15SpecifyMachine queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入斜裁定点机台")
    @PostMapping("/specifyMachine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
