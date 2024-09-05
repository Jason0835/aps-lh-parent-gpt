package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.lh.api.domain.entity.MixMachineInfo;


/**
 * 密炼机台信息Service接口
 * @author zlt
 * @date 2021-11-09
 */
@FeignClient(contextId = "IMixMachineInfoService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixMachineInfoService {

    /**
     * 查询密炼机台信息列表
     */
    @ApiOperation("查询密炼机台信息列表")
    @PostMapping("/mixMachine/list")
    TableDataInfo list(@RequestBody MixMachineInfo mixMachineInfo);

    /**
    * 新增密炼机台信息
    */
    @ApiOperation("新增密炼机台信息")
    @PostMapping("/mixMachine/add")
    AjaxResult add(@RequestBody MixMachineInfo mixMachineInfo);

    /**
     * 修改密炼机台信息
     */
    @ApiOperation("修改密炼机台信息")
    @PostMapping("/mixMachine/edit")
    AjaxResult edit(@RequestBody MixMachineInfo mixMachineInfo);

    /**
     * 删除密炼机台信息
     */
    @ApiOperation("删除密炼机台信息")
    @DeleteMapping("/mixMachine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mixMachine/{id}")
    MixMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验密炼机台信息唯一性
     */
    @ApiOperation("校验密炼机台信息唯一性")
    @PostMapping("/mixMachine/checkMixMachineInfoUnique")
    String checkMixMachineInfoUnique(@RequestBody MixMachineInfo mixMachineInfo);

    /**
     * 导出密炼机台信息列表
     */
    @ApiOperation("导出密炼机台信息列表")
    @PostMapping("/mixMachine/getList")
    List<MixMachineInfo> getList(@RequestBody MixMachineInfo mixMachineInfo);

    /**
     * 导入密炼机台信息数据
     */
    @ApiOperation("导入密炼机台信息")
    @PostMapping("/mixMachine/importData")
    public AjaxResult importData(@RequestBody List<MixMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
