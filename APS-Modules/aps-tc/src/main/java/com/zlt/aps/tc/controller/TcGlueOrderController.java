package com.zlt.aps.tc.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.dto.TcGlueOrderDto;
import com.zlt.aps.tc.entity.TcGlueOrder;
import com.zlt.aps.tc.service.TcGlueOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@Api(tags = {"胎侧胶料顺序维护接口"})
@RestController
@RequestMapping("/glueOrder")
public class TcGlueOrderController extends BaseController {

    @Resource
    public TcGlueOrderService tcGlueOrderService;

    @ApiOperation("根据条件查询胶料顺序列表")
    @GetMapping("/listGlueOrder")
    public TableDataInfo listGlueOrder(TcGlueOrderDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<TcGlueOrderDto> list = tcGlueOrderService.listGlueOrder(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询胶料顺序信息")
    @GetMapping("/getGlueOrder/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TcGlueOrderDto getGlueOrder(@PathVariable("id") Long id) {
        TcGlueOrderDto dto = new TcGlueOrderDto();
        BeanUtils.copyProperties(tcGlueOrderService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.tc.glueOrder.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存胶料顺序信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveGlueOrder")
    public AjaxResult saveGlueOrder(@RequestBody TcGlueOrderDto dto) {
        TcGlueOrder entity = new TcGlueOrder();
        BeanUtils.copyProperties(dto, entity);
        tcGlueOrderService.saveGlueOrder(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断胶料组号是否已经存在")
    @PostMapping("/checkGlueCodeUnique")
    public String checkGlueCodeUnique(@RequestBody TcGlueOrderDto dto) {
        return tcGlueOrderService.checkGlueCodeUnique(dto);
    }

    @Log(title = "ui.tc.glueOrder.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除胶料顺序信息(逻辑删)")
    @PostMapping("/deleteGlueOrder/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueOrder(@PathVariable("ids") Long[] ids) {
        tcGlueOrderService.deleteGlueOrder(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.tc.glueOrder.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<TcGlueOrderDto> exportData(TcGlueOrderDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<TcGlueOrderDto> list = tcGlueOrderService.listGlueOrder(dto);
        return list;
    }

    @Log(title = "ui.tc.glueOrder.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TcGlueOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tcGlueOrderService.importData(list, updateSupport, importLogId);
    }
}
