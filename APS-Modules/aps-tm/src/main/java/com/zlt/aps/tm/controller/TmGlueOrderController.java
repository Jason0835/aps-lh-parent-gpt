package com.zlt.aps.tm.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.dto.TmGlueOrderDto;
import com.zlt.aps.tm.entity.TmGlueOrder;
import com.zlt.aps.tm.service.TmGlueOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@Api(tags = {"胎面胶料顺序维护接口"})
@RestController
@RequestMapping("/glueOrder")
public class TmGlueOrderController extends BaseController {

    @Resource
    public TmGlueOrderService tmGlueOrderService;

    @ApiOperation("根据条件查询胶料顺序列表")
    @GetMapping("/listGlueOrder")
    public TableDataInfo listGlueOrder(TmGlueOrderDto dto) {
        startPage();  //分页并排序
        dto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TmGlueOrderDto> list = tmGlueOrderService.listGlueOrder(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询胶料顺序信息")
    @GetMapping("/getGlueOrder/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TmGlueOrderDto getGlueOrder(@PathVariable("id") Long id) {
        TmGlueOrderDto dto = new TmGlueOrderDto();
        BeanUtils.copyProperties(tmGlueOrderService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.tm.glueOrder.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存胶料顺序信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveGlueOrder")
    public AjaxResult saveGlueOrder(@RequestBody TmGlueOrderDto dto) {
        TmGlueOrder entity = new TmGlueOrder();
        BeanUtils.copyProperties(dto, entity);
        tmGlueOrderService.saveGlueOrder(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断胶料组号是否已经存在")
    @PostMapping("/checkGlueCodeUnique")
    public String checkGlueCodeUnique(@RequestBody TmGlueOrderDto dto) {
        return tmGlueOrderService.checkGlueCodeUnique(dto);
    }

    @Log(title = "ui.tm.glueOrder.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除胶料顺序信息(逻辑删)")
    @PostMapping("/deleteGlueOrder/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueOrder(@PathVariable("ids") Long[] ids) {
        tmGlueOrderService.deleteGlueOrder(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.tm.glueOrder.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<TmGlueOrderDto> exportData(TmGlueOrderDto dto) {
        dto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TmGlueOrderDto> list = tmGlueOrderService.listGlueOrder(dto);
        return list;
    }

    @Log(title = "ui.tm.glueOrder.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎面胶料顺序信息")
    public AjaxResult importData(@RequestBody List<TmGlueOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tmGlueOrderService.importData(list,updateSupport, importLogId);
    }
}
