package com.zlt.aps.dj.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.service.DjMachineInfoService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * 垫胶机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "垫胶机台信息维护接口")
@RestController
@RequestMapping("/dj/machine")
public class DjMachineInfoController extends BaseController {
    @Autowired
    private DjMachineInfoService machineInfoService;

    /**
     * 查询垫胶机台信息列表
     */
    @ApiOperation("根据条件查询垫胶机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody DjMachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<DjMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return getDataTable(list);
    }

    /**
     * 获取垫胶机台信息详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询垫胶机台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public DjMachineInfo getInfo(@PathVariable("id") Long id) {
        return machineInfoService.selectMachineInfoById(id);
    }

    /**
     * 新增垫胶机台信息
     */
    //@PreAuthorize(hasPermi = "tc:machine:add")
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT)
    @ApiOperation("新增垫胶机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody DjMachineInfo machineInfo) {
        return toAjax(machineInfoService.insertMachineInfo(machineInfo));
    }

    /**
     * 修改垫胶机台信息
     */
    //@PreAuthorize(hasPermi = "tc:machine:edit")
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.UPDATE)
    @ApiOperation("修改垫胶机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody DjMachineInfo machineInfo) {
        return toAjax(machineInfoService.updateMachineInfo(machineInfo));
    }

    /**
     * 删除垫胶机台信息
     */
    //@PreAuthorize(hasPermi = "tc:machine:remove")
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除垫胶机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(machineInfoService.deleteMachineInfoByIds(ids));
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody DjMachineInfo machineInfo) {
        return machineInfoService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<DjMachineInfo> exportList(@RequestBody DjMachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<DjMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return list;
    }

    /**
     * 根据垫胶和口型板获取对应机台信息
     */
    @PostMapping("/list2")
    public List<DjMachineInfo> list2(@RequestBody DjMachineInfo machineInfo) {
        List<DjMachineInfo> list = machineInfoService.selectMachineInfoList2(machineInfo);
        return list;
    }

    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入垫胶机台信息")
    public AjaxResult importData(@RequestBody List<DjMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return machineInfoService.importData(list, updateSupport, importLogId);
    }
}
