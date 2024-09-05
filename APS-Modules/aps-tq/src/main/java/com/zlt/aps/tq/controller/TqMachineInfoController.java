package com.zlt.aps.tq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.service.TqMachineInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "胎圈机台信息维护接口")
@RestController
@RequestMapping("/machine")
public class TqMachineInfoController extends BaseController {
    @Autowired
    private TqMachineInfoService machineInfoService;

    /**
     * 查询胎圈机台信息列表
     */
    @ApiOperation("根据条件查询胎圈机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TqMachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<TqMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return getDataTable(list);
    }

    /**
     * 获取胎圈机台信息详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询胎圈机台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TqMachineInfo getInfo(@PathVariable("id") Long id) {
        return machineInfoService.selectMachineInfoById(id);
    }

    /**
     * 新增胎圈机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT)
    @ApiOperation("新增胎圈机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody TqMachineInfo machineInfo) {
        return toAjax(machineInfoService.insertMachineInfo(machineInfo));
    }

    /**
     * 修改胎圈机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胎圈机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody TqMachineInfo machineInfo) {
        return toAjax(machineInfoService.updateMachineInfo(machineInfo));
    }

    /**
     * 删除胎圈机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除胎圈机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(machineInfoService.deleteMachineInfoByIds(ids));
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody TqMachineInfo machineInfo) {
        return machineInfoService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 导出胎圈机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎圈机台信息")
    @PostMapping("/exportList")
    public List<TqMachineInfo> exportList(@RequestBody TqMachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<TqMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return list;
    }

    /**
     * 根据条件查询胎圈机台信息
     */
    @ApiOperation("根据条件查询胎圈机台信息")
    @PostMapping("/listMachineInfo")
    public List<TqMachineInfo> listMachineInfo(@RequestBody TqMachineInfo machineInfo) {
        return machineInfoService.listMachineInfo(machineInfo);
    }

    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎圈机台信息")
    public AjaxResult importData(@RequestBody List<TqMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return machineInfoService.importData(list, updateSupport, importLogId);
    }
}
