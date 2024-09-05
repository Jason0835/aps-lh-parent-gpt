package com.zlt.aps.tm.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.dto.TmGlueGroupOrderDto;
import com.zlt.aps.tm.entity.TmGlueGroupOrder;
import com.zlt.aps.tm.service.TmGlueGroupOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 胎面胶料组别顺序维护 前端控制器
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-25
 */
@Api(tags = {"胎面胶料组别顺序维护接口"})
@RestController
@RequestMapping("/glueGroupOrder")
public class TmGlueGroupOrderController extends BaseController {

    @Resource
    public TmGlueGroupOrderService tmGlueGroupOrderService;

    @ApiOperation("根据条件查询胶料组别顺序列表")
    @GetMapping("/listGlueGroupOrder")
    public TableDataInfo listGlueGroupOrder(TmGlueGroupOrderDto dto) {
        startPage();
        dto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TmGlueGroupOrderDto> list = tmGlueGroupOrderService.listGlueGroupOrder(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询胶料组别顺序信息")
    @GetMapping("/getGlueGroupOrder/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TmGlueGroupOrderDto getGlueGroupOrder(@PathVariable("id") Long id) {
        TmGlueGroupOrderDto dto = new TmGlueGroupOrderDto();
        BeanUtils.copyProperties(tmGlueGroupOrderService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.tm.glueGroup.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存胶料组别顺序信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveGlueGroupOrder")
    public AjaxResult saveGlueGroupOrder(@RequestBody TmGlueGroupOrderDto dto) {
        TmGlueGroupOrder entity = new TmGlueGroupOrder();
        BeanUtils.copyProperties(dto, entity);
        tmGlueGroupOrderService.saveGlueGroupOrder(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断胶料组号是否已经存在")
    @PostMapping("/checkGlueGroupCodeUnique")
    public String checkGlueGroupCodeUnique(@RequestBody TmGlueGroupOrderDto dto) {
        return tmGlueGroupOrderService.checkGlueGroupCodeUnique(dto);
    }

    @Log(title = "ui.tm.glueGroup.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除胶料组别顺序信息(逻辑删)")
    @PostMapping("/deleteGlueGroupOrder/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueGroupOrder(@PathVariable("ids") Long[] ids) {
        tmGlueGroupOrderService.deleteGlueGroupOrder(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.tm.glueGroup.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<TmGlueGroupOrderDto> exportData(TmGlueGroupOrderDto dto) {
        startPage();
        dto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TmGlueGroupOrderDto> list = tmGlueGroupOrderService.listGlueGroupOrder(dto);
        return list;
    }

    @Log(title = "ui.tm.glueGroup.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎面胶料组别顺序信息")
    public AjaxResult importData(@RequestBody List<TmGlueGroupOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tmGlueGroupOrderService.importData(list,updateSupport, importLogId);
    }
}
