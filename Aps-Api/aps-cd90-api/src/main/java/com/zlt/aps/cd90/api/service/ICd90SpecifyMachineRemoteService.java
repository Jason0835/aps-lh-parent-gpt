package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90SpecifyMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 直裁定点机台 Feign 接口。
 */
@FeignClient(contextId = "ICd90SpecifyMachineRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90SpecifyMachineRemoteService {

    /** 查询列表 */
    @ApiOperation("查询直裁定点机台列表")
    @PostMapping("/specifyMachine/list")
    TableDataInfo list(@RequestBody Cd90SpecifyMachine queryVO);

    /** 获取详情 */
    @ApiOperation("获取直裁定点机台详情")
    @GetMapping("/specifyMachine/getInfo/{id}")
    Cd90SpecifyMachine getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增直裁定点机台")
    @PostMapping("/specifyMachine/add")
    AjaxResult add(@RequestBody Cd90SpecifyMachine specifyMachine);

    /** 编辑 */
    @ApiOperation("编辑直裁定点机台")
    @PostMapping("/specifyMachine/edit")
    AjaxResult edit(@RequestBody Cd90SpecifyMachine specifyMachine);

    /** 删除 */
    @ApiOperation("删除直裁定点机台")
    @PostMapping("/specifyMachine/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 清空 */
    @ApiOperation("清空直裁定点机台")
    @PostMapping("/specifyMachine/removeAll")
    AjaxResult removeAll(@RequestBody Cd90SpecifyMachine queryVO);

    /** 校验唯一性 */
    @ApiOperation("校验直裁定点机台唯一性")
    @PostMapping("/specifyMachine/checkUnique")
    String checkUnique(@RequestBody Cd90SpecifyMachine specifyMachine);

    /** 导出数据 */
    @ApiOperation("导出直裁定点机台")
    @PostMapping("/specifyMachine/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90SpecifyMachine queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入直裁定点机台")
    @PostMapping("/specifyMachine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
