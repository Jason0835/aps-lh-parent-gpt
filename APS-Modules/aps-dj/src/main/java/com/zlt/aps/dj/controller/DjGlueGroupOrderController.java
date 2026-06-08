package com.zlt.aps.dj.controller;


import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.zlt.aps.dj.api.domain.dto.DjGlueGroupOrderDto;
import com.zlt.aps.dj.api.domain.entity.DjGlueGroupOrder;
import com.zlt.aps.dj.service.DjGlueGroupOrderService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * <p>
 * 垫胶胶料组别顺序维护 前端控制器
 * </p>
 *
 * @author zhangbinglin
 */
@Api(tags = {"垫胶胶料组别顺序维护接口"})
@RestController
@RequestMapping("/dj/glueGroupOrder")
public class DjGlueGroupOrderController extends BaseController {

    @Resource
    public DjGlueGroupOrderService NcGlueGroupOrderService;

    @ApiOperation("根据条件查询胶料组别顺序列表")
    @PostMapping("/listGlueGroupOrder")
    public TableDataInfo listGlueGroupOrder(@RequestBody DjGlueGroupOrderDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<DjGlueGroupOrderDto> list = NcGlueGroupOrderService.listGlueGroupOrder(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询胶料组别顺序信息")
    @GetMapping("/getGlueGroupOrder/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public DjGlueGroupOrderDto getGlueGroupOrder(@PathVariable("id") Long id) {
        DjGlueGroupOrderDto dto = new DjGlueGroupOrderDto();
        BeanUtils.copyProperties(NcGlueGroupOrderService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.nc.glueGroup.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存胶料组别顺序信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveGlueGroupOrder")
    public AjaxResult saveGlueGroupOrder(@RequestBody DjGlueGroupOrderDto dto) {
        DjGlueGroupOrder entity = new DjGlueGroupOrder();
        BeanUtils.copyProperties(dto, entity);
        NcGlueGroupOrderService.saveGlueGroupOrder(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断胶料组号是否已经存在")
    @PostMapping("/checkGlueGroupCodeUnique")
    public String checkGlueGroupCodeUnique(@RequestBody DjGlueGroupOrderDto dto) {
        return NcGlueGroupOrderService.checkGlueGroupCodeUnique(dto);
    }

    @Log(title = "ui.nc.glueGroup.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除胶料组别顺序信息(逻辑删)")
    @PostMapping("/deleteGlueGroupOrder/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueGroupOrder(@PathVariable("ids") Long[] ids) {
        NcGlueGroupOrderService.deleteGlueGroupOrder(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.nc.glueGroup.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<DjGlueGroupOrderDto> exportData(@RequestBody DjGlueGroupOrderDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<DjGlueGroupOrderDto> list = NcGlueGroupOrderService.listGlueGroupOrder(dto);
        return list;
    }

    @Log(title = "ui.nc.glueGroup.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<DjGlueGroupOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return NcGlueGroupOrderService.importData(list,updateSupport, importLogId);
    }
}
