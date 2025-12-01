package com.zlt.aps.nc.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.dto.NcGlueOrderDto;
import com.zlt.aps.nc.entity.NcGlueOrder;
import com.zlt.aps.nc.service.NcGlueOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@Api(tags = {"内衬胶料顺序维护接口"})
@RestController
@RequestMapping("/nc/glueOrder")
public class NcGlueOrderController extends BaseController {

    @Resource
    public NcGlueOrderService NcGlueOrderService;

    @ApiOperation("根据条件查询胶料顺序列表")
    @PostMapping("/listGlueOrder")
    public TableDataInfo listGlueOrder(@RequestBody NcGlueOrderDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<NcGlueOrderDto> list = NcGlueOrderService.listGlueOrder(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询胶料顺序信息")
    @GetMapping("/getGlueOrder/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public NcGlueOrderDto getGlueOrder(@PathVariable("id") Long id) {
        NcGlueOrderDto dto = new NcGlueOrderDto();
        BeanUtils.copyProperties(NcGlueOrderService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.nc.glueOrder.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存胶料顺序信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveGlueOrder")
    public AjaxResult saveGlueOrder(@RequestBody NcGlueOrderDto dto) {
        NcGlueOrder entity = new NcGlueOrder();
        BeanUtils.copyProperties(dto, entity);
        NcGlueOrderService.saveGlueOrder(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断胶料组号是否已经存在")
    @PostMapping("/checkGlueCodeUnique")
    public String checkGlueCodeUnique(@RequestBody NcGlueOrderDto dto) {
        return NcGlueOrderService.checkGlueCodeUnique(dto);
    }

    @Log(title = "ui.nc.glueOrder.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除胶料顺序信息(逻辑删)")
    @PostMapping("/deleteGlueOrder/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueOrder(@PathVariable("ids") Long[] ids) {
        NcGlueOrderService.deleteGlueOrder(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.nc.glueOrder.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<NcGlueOrderDto> exportData(@RequestBody NcGlueOrderDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<NcGlueOrderDto> list = NcGlueOrderService.listGlueOrder(dto);
        return list;
    }

    @Log(title = "ui.nc.glueOrder.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入内衬胶料顺序信息")
    public AjaxResult importData(@RequestBody List<NcGlueOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return NcGlueOrderService.importData(list,updateSupport, importLogId);
    }
}
