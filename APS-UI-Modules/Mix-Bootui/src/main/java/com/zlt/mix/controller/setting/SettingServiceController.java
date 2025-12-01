package com.zlt.mix.controller.setting;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.service.SettingService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 基础资料服务Controller
 *
 * @author hak
 * @date 2024-12-26
 */
@Api(tags = "基础资料服务")
@Controller
@RequestMapping("/setting/service")
public class SettingServiceController extends BaseController {

    @Resource
    private SettingService settingService;

    /**
     * 机台下拉列表
     */
//    @RequiresPermissions("setting:scheduleParams:list" )
    @GetMapping("/getMachineInfo")
    @ResponseBody
    @ApiOperation("机台下拉列表")
    public AjaxResult getMachineInfo() {
        return AjaxResult.success(settingService.getMachineInfo());
    }

    /**
     * 机台下拉列表
     */
    @GetMapping("/getMachineInfo/{mixArea}")
    @ResponseBody
    @ApiOperation("机台下拉列表")
    public AjaxResult getMachineInfo(@PathVariable("mixArea") String mixArea) {
        return AjaxResult.success(settingService.getMachineInfo(mixArea));
    }

    /**
     * 机台下拉列表
     */
    @GetMapping("/getEnableMachineInfo")
    @ResponseBody
    @ApiOperation("机台下拉列表")
    public AjaxResult getEnableMachineInfo() {
        return AjaxResult.success(settingService.getEnableMachineInfo());
    }

    /**
     * 获取启用的密炼机台下拉列表
     */
    @GetMapping("/getEnableMachineInfo/{mixArea}")
    @ResponseBody
    @ApiOperation("机台下拉列表")
    public AjaxResult getEnableMachineInfo(@PathVariable("mixArea") String mixArea) {
        return AjaxResult.success(settingService.getEnableMachineInfo(mixArea));
    }

    /**
     * 硫磺辅料机台下拉列表
     */
    @GetMapping("/getLhflMachineInfo")
    @ResponseBody
    @ApiOperation("硫磺辅料机台下拉列表")
    public AjaxResult getLhflMachineInfo() {
        return AjaxResult.success(settingService.getLhflMachineInfo());
    }

    /**
     * 硫磺辅料机台下拉列表
     */
    @GetMapping("/getLhflMachineInfo/{mixArea}")
    @ResponseBody
    @ApiOperation("硫磺辅料机台下拉列表")
    public AjaxResult getLhflMachineInfo(@PathVariable("mixArea") String mixArea) {
        return AjaxResult.success(settingService.getLhflMachineInfo(mixArea));
    }

    /**
     * 硫磺辅料机台下拉列表
     */
    @GetMapping("/getEnableLhflMachineInfo")
    @ResponseBody
    @ApiOperation("硫磺辅料机台下拉列表")
    public AjaxResult getEnableLhflMachineInfo() {
        return AjaxResult.success(settingService.getEnableLhflMachineInfo());
    }

    /**
     * 硫磺辅料机台下拉列表
     */
    @GetMapping("/getEnableLhflMachineInfo/{mixArea}")
    @ResponseBody
    @ApiOperation("硫磺辅料机台下拉列表")
    public AjaxResult getEnableLhflMachineInfo(@PathVariable("mixArea") String mixArea) {
        return AjaxResult.success(settingService.getEnableLhflMachineInfo(mixArea));
    }

    /**
     * 胶料名称列表
     */
    @GetMapping("/getFinalGlueNames")
    @ResponseBody
    @ApiOperation("胶料名称列表")
    public AjaxResult getFinalGlueNames() {
        return AjaxResult.success(settingService.getFinalGlueNames());
    }

    /**
     * 胶料名称列表
     */
    @GetMapping("/getGlueNames")
    @ResponseBody
    @ApiOperation("胶料名称列表")
    public AjaxResult getGlueNames() {
        return AjaxResult.success(settingService.getGlueNames());
    }

    /**
     * 辅料名称列表
     */
    @GetMapping("/getAccessoriesNames")
    @ResponseBody
    @ApiOperation("辅料名称列表")
    public AjaxResult getAccessoriesNames() {
        return AjaxResult.success(settingService.getAccessoriesNames());
    }

    /**
     * 物料名称列表
     */
    @GetMapping("/getMaterialNames")
    @ResponseBody
    @ApiOperation("物料名称列表")
    public AjaxResult getMaterialNames() {
        return AjaxResult.success(settingService.getMaterialNames());
    }

    /**
     * 获取密炼排程的当前用户的密炼区权限字典
     */
    @GetMapping("/scheduleMixAreaPermission")
    @ResponseBody
    @ApiOperation("获取密炼排程的当前用户的密炼区权限字典")
    public AjaxResult scheduleMixAreaPermission() {
        return AjaxResult.success(settingService.scheduleMixAreaPermission());
    }

    /**
     * 查询所有机台信息(包含硫磺辅料机台信息)
     * 
     * @return 查询到的机台信息
     */
    @GetMapping("/getAllMachineInfo")
    @ResponseBody
    @ApiOperation("查询所有机台信息(包含硫磺辅料机台信息)")
    public AjaxResult getAllMachineInfo() {
        return AjaxResult.success(settingService.getAllMachineInfo());
    }
}
