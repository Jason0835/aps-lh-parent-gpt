package com.zlt.mix.setting.controller;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.zlt.mix.setting.service.MixMachineService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 密炼机台信息Controller
 *
 * @author Gim
 * @date 2022-03-22
 */
@RestController
@RequestMapping("/machine")
public class MixMachineController extends BaseController {
    @Resource
    private MixMachineService mixMachineService;

    /**
     * 查询密炼机台信息列表
     */
    @ApiOperation("查询密炼机台信息列表")
    @PostMapping("/list")
    public TableDataInfo listMixMachine(@RequestBody MixMachine mixMachine) {
        startPage(false);
        mixMachine.setOrderStr(orderStr());
        List<MixMachine> list = mixMachineService.selectMixMachineList(mixMachine);
        return getDataTable(list);
    }

    @ApiOperation("获取密炼机台信息详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public MixMachine getMixMachineInfo(@PathVariable("id") Long id){
        return mixMachineService.getById(id);
    }

    @Log(title = "setting.machine.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存密炼机台信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveMixMachine(@RequestBody MixMachine mixMachine) {
        mixMachineService.saveMixMachine(mixMachine);
        return AjaxResult.success();
    }

    @Log(title = "setting.machine.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除密炼机台信息")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteMixMachine(@PathVariable Long[] ids){
        return toAjax(mixMachineService.deleteMixMachineByIds(ids));
    }

    @ApiOperation("取出密炼机台信息列表")
    @PostMapping("/getMachines")
    public List<MixMachine> getMachines(@RequestBody MixMachine mixMachine){
        startPage(false);
        mixMachine.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  mixMachineService.selectMixMachineList(mixMachine);
    }

    @Log(title = "setting.machine.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出密炼机台信息列表")
    @PostMapping("/exportData")
    public List<MixMachine> exportData(@RequestBody MixMachine mixMachine){
        startPage(false);
        mixMachine.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  mixMachineService.selectMixMachineList(mixMachine);
    }

    @ApiOperation("校验密炼机台信息唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkMixMachineUnique")
    public String checkMixMachineUnique(@RequestBody MixMachine mixMachine){
        return mixMachineService.checkMixMachineUnique(mixMachine);
    }

    @Log(title = "setting.machine.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入密炼机台信息数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return mixMachineService.importData(list, updateSupport, importLogId);
    }

    /**
     * 查询所有机台信息(包含硫磺辅料机台信息)
     * @return 查询到的机台信息
     */
    @ApiOperation("查询所有机台信息(包含硫磺辅料机台信息)")
    @PostMapping("/getAllMachineInfo")
    public ArrayList<MixMachine> getAllMachineInfo() {
        return mixMachineService.getAllMachineInfo();
    }
}
