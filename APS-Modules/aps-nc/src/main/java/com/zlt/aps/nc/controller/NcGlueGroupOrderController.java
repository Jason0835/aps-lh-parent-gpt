package com.zlt.aps.nc.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.nc.api.domain.dto.NcGlueGroupOrderDto;
import com.zlt.aps.nc.entity.NcGlueGroupOrder;
import com.zlt.aps.nc.service.NcGlueGroupOrderService;
import io.swagger.annotations.Api;
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
 * 内衬胶料组别顺序维护 前端控制器
 * </p>
 *
 * @author zhangbinglin
 */
@Api(tags = {"内衬胶料组别顺序维护接口"})
@RestController
@RequestMapping("/nc/glueGroupOrder")
public class NcGlueGroupOrderController extends BaseController {

    @Resource
    public NcGlueGroupOrderService NcGlueGroupOrderService;

    @ApiOperation("根据条件查询胶料组别顺序列表")
    @PostMapping("/listGlueGroupOrder")
    public TableDataInfo listGlueGroupOrder(@RequestBody NcGlueGroupOrderDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<NcGlueGroupOrderDto> list = NcGlueGroupOrderService.listGlueGroupOrder(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询胶料组别顺序信息")
    @GetMapping("/getGlueGroupOrder/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public NcGlueGroupOrderDto getGlueGroupOrder(@PathVariable("id") Long id) {
        NcGlueGroupOrderDto dto = new NcGlueGroupOrderDto();
        BeanUtils.copyProperties(NcGlueGroupOrderService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.nc.glueGroup.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存胶料组别顺序信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveGlueGroupOrder")
    public AjaxResult saveGlueGroupOrder(@RequestBody NcGlueGroupOrderDto dto) {
        NcGlueGroupOrder entity = new NcGlueGroupOrder();
        BeanUtils.copyProperties(dto, entity);
        NcGlueGroupOrderService.saveGlueGroupOrder(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断胶料组号是否已经存在")
    @PostMapping("/checkGlueGroupCodeUnique")
    public String checkGlueGroupCodeUnique(@RequestBody NcGlueGroupOrderDto dto) {
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
    public List<NcGlueGroupOrderDto> exportData(@RequestBody NcGlueGroupOrderDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<NcGlueGroupOrderDto> list = NcGlueGroupOrderService.listGlueGroupOrder(dto);
        return list;
    }

    @Log(title = "ui.nc.glueGroup.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<NcGlueGroupOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return NcGlueGroupOrderService.importData(list,updateSupport, importLogId);
    }
}
