package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 斜裁大卷与机台映射 Feign 接口。
 */
@FeignClient(contextId = "ICd15MachineRollMappingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15MachineRollMappingRemoteService {

    /** 查询列表 */
    @ApiOperation("查询斜裁大卷与机台映射列表")
    @PostMapping("/cd15MachineRollMapping/list")
    TableDataInfo list(@RequestBody Cd15MachineRollMapping queryVO);

    /** 获取详情 */
    @ApiOperation("获取斜裁大卷与机台映射详情")
    @GetMapping("/cd15MachineRollMapping/getInfo/{id}")
    Cd15MachineRollMapping getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增斜裁大卷与机台映射")
    @PostMapping("/cd15MachineRollMapping/add")
    AjaxResult add(@RequestBody Cd15MachineRollMapping entity);

    /** 编辑 */
    @ApiOperation("编辑斜裁大卷与机台映射")
    @PostMapping("/cd15MachineRollMapping/edit")
    AjaxResult edit(@RequestBody Cd15MachineRollMapping entity);

    /** 删除 */
    @ApiOperation("删除斜裁大卷与机台映射")
    @PostMapping("/cd15MachineRollMapping/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 清空 */
    @ApiOperation("清空斜裁大卷与机台映射")
    @PostMapping("/cd15MachineRollMapping/removeAll")
    AjaxResult removeAll(@RequestBody Cd15MachineRollMapping queryVO);

    /** 校验唯一性 */
    @ApiOperation("校验斜裁大卷与机台映射唯一性")
    @PostMapping("/cd15MachineRollMapping/checkUnique")
    String checkUnique(@RequestBody Cd15MachineRollMapping entity);

    /** 导出数据 */
    @ApiOperation("导出斜裁大卷与机台映射")
    @PostMapping("/cd15MachineRollMapping/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15MachineRollMapping queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入斜裁大卷与机台映射")
    @PostMapping("/cd15MachineRollMapping/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
