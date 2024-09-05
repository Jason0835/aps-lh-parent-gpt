package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;
import com.zlt.aps.xwyy.service.XwyyMachineInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "纤维压延机台信息维护接口")
@RestController
@RequestMapping("/machine")
public class XwyyMachineInfoController extends BaseController {
    @Autowired
    private XwyyMachineInfoService machineInfoService;

    /**
     * 查询纤维压延机台信息列表
     */
    @ApiOperation("根据条件查询纤维压延机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyMachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<XwyyMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return getDataTable(list);
    }

    /**
     * 获取纤维压延机台信息详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询纤维压延机台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public XwyyMachineInfo getInfo(@PathVariable("id") Long id) {
        return machineInfoService.selectMachineInfoById(id);
    }

    /**
     * 新增纤维压延机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("新增纤维压延机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody XwyyMachineInfo machineInfo) {
        return toAjax(machineInfoService.insertMachineInfo(machineInfo));
    }

    /**
     * 修改纤维压延机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("修改纤维压延机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody XwyyMachineInfo machineInfo) {
        return toAjax(machineInfoService.updateMachineInfo(machineInfo));
    }

    /**
     * 删除纤维压延机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除纤维压延机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(machineInfoService.deleteMachineInfoByIds(ids));
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody XwyyMachineInfo machineInfo) {
        return machineInfoService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 导出纤维压延机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @ApiOperation("导出纤维压延机台信息")
    @PostMapping("/exportList")
    public List<XwyyMachineInfo> exportList(@RequestBody XwyyMachineInfo machineInfo) {
        machineInfo.setOrderStr(orderStr());
        List<XwyyMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return list;
    }

    /**
     * 查询帘布大卷和机台映射信息
     */
    @ApiOperation("查询帘布大卷和机台映射信息")
    @PostMapping("/listMachineInfo")
    public List<XwyyMachineInfo> listMachineInfo(@RequestBody XwyyMachineInfo machineInfo) {
        return machineInfoService.listMachineInfo(machineInfo);
    }

    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<XwyyMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return machineInfoService.importData(list, updateSupport, importLogId);
    }
}
