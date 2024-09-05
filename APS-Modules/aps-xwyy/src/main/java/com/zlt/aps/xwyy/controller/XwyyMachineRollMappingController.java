package com.zlt.aps.xwyy.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.dto.XwyyMachineRollMappingDto;
import com.zlt.aps.xwyy.entity.XwyyMachineRollMapping;
import com.zlt.aps.xwyy.service.XwyyMachineRollMappingService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 纤维压延帘布大卷与机台的映射表 前端控制器
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@RestController
@RequestMapping("/xwyyMachineRollMapping")
public class XwyyMachineRollMappingController extends BaseController {
    @Resource
    public XwyyMachineRollMappingService xwyyMachineRollMappingService;

    @ApiOperation("根据条件查询纤维压延帘布大卷与机台的映射表列表")
    @PostMapping("/listXwyyMachineRollMapping")
    public TableDataInfo listXwyyMachineRollMapping(@RequestBody XwyyMachineRollMappingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<XwyyMachineRollMappingDto> list = xwyyMachineRollMappingService.listXwyyMachineRollMapping(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询纤维压延帘布大卷与机台的映射表列表")
    @GetMapping("/getXwyyMachineRollMapping/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public XwyyMachineRollMappingDto getXwyyMachineRollMapping(@PathVariable("id") Long id) {
        XwyyMachineRollMappingDto dto = new XwyyMachineRollMappingDto();
        BeanUtils.copyProperties(xwyyMachineRollMappingService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.MachineRollMapping.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存帘布大卷颜色提示信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveXwyyMachineRollMapping")
    public AjaxResult saveXwyyMachineRollMapping(@RequestBody XwyyMachineRollMappingDto dto) {
        XwyyMachineRollMapping entity = new XwyyMachineRollMapping();
        BeanUtils.copyProperties(dto, entity);
        xwyyMachineRollMappingService.saveXwyyMachineRollMapping(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断主键是否已经存在")
    @PostMapping("/checkXwyyMachineRollMapping")
    public String checkXwyyMachineRollMapping(@RequestBody XwyyMachineRollMappingDto dto) {
        return xwyyMachineRollMappingService.checkXwyyMachineRollMapping(dto);
    }

    @Log(title = "ui.MachineRollMapping.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除帘布大卷颜色提示信息(逻辑删)")
    @PostMapping("/deleteXwyyMachineRollMapping/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteXwyyMachineRollMapping(@PathVariable("ids") Long[] ids) {
        xwyyMachineRollMappingService.deleteXwyyMachineRollMapping(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.MachineRollMapping.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<XwyyMachineRollMappingDto> exportData(XwyyMachineRollMappingDto dto) {
        dto.setOrderStr(orderStr());
        List<XwyyMachineRollMappingDto> list = xwyyMachineRollMappingService.listXwyyMachineRollMapping(dto);
        return list;
    }

    @Log(title = "ui.MachineRollMapping.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<XwyyMachineRollMappingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyMachineRollMappingService.importData(list, updateSupport, importLogId);
    }
}
