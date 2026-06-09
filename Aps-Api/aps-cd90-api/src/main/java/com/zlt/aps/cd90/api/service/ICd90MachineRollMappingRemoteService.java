package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 直裁大卷与机台映射 Feign 接口。
 */
@FeignClient(contextId = "ICd90MachineRollMappingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90MachineRollMappingRemoteService {

    /** 查询列表 */
    @ApiOperation("查询直裁大卷与机台映射列表")
    @PostMapping("/cd90MachineRollMapping/list")
    TableDataInfo list(@RequestBody Cd90MachineRollMapping queryVO);

    /** 获取详情 */
    @ApiOperation("获取直裁大卷与机台映射详情")
    @GetMapping("/cd90MachineRollMapping/getInfo/{id}")
    Cd90MachineRollMapping getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增直裁大卷与机台映射")
    @PostMapping("/cd90MachineRollMapping/add")
    AjaxResult add(@RequestBody Cd90MachineRollMapping entity);

    /** 编辑 */
    @ApiOperation("编辑直裁大卷与机台映射")
    @PostMapping("/cd90MachineRollMapping/edit")
    AjaxResult edit(@RequestBody Cd90MachineRollMapping entity);

    /** 删除 */
    @ApiOperation("删除直裁大卷与机台映射")
    @PostMapping("/cd90MachineRollMapping/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 清空 */
    @ApiOperation("清空直裁大卷与机台映射")
    @PostMapping("/cd90MachineRollMapping/removeAll")
    AjaxResult removeAll(@RequestBody Cd90MachineRollMapping queryVO);

    /** 校验唯一性 */
    @ApiOperation("校验直裁大卷与机台映射唯一性")
    @PostMapping("/cd90MachineRollMapping/checkUnique")
    String checkUnique(@RequestBody Cd90MachineRollMapping entity);

    /** 导出数据 */
    @ApiOperation("导出直裁大卷与机台映射")
    @PostMapping("/cd90MachineRollMapping/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90MachineRollMapping queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入直裁大卷与机台映射")
    @PostMapping("/cd90MachineRollMapping/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
