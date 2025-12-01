package com.zlt.aps.tm.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.entity.TmAssistSpec;
import com.zlt.aps.tm.service.TmAssistSpecService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"胎面外协规格管理接口"})
@RestController
@RequestMapping("/tm/assistSpec")
public class TmAssistSpecController extends BaseController {

    @Resource
    private TmAssistSpecService tmAssistSpecService;

    @ApiOperation("根据条件查询外协规格管理列表")
    @PostMapping("/listAssistSpec")
    public TableDataInfo listAssistSpec(@RequestBody TmAssistSpec dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<TmAssistSpec> list = tmAssistSpecService.listAssistSpec(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询外协规格管理信息")
    @GetMapping("/getAssistSpec/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TmAssistSpec getAssistSpec(@PathVariable("id") Long id) {
        TmAssistSpec dto = new TmAssistSpec();
        BeanUtils.copyProperties(tmAssistSpecService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.tm.assistSpec.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存外协规格管理信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveAssistSpec")
    public AjaxResult saveAssistSpec(@RequestBody TmAssistSpec dto) {
        TmAssistSpec entity = new TmAssistSpec();
        BeanUtils.copyProperties(dto, entity);
        tmAssistSpecService.saveAssistSpec(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断代号是否已经存在")
    @PostMapping("/checkAssistSpecCodeUnique")
    public String checkAssistSpecCodeUnique(@RequestBody TmAssistSpec dto) {
        return tmAssistSpecService.checkAssistSpecCodeUnique(dto);
    }

    @Log(title = "ui.tm.assistSpec.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除外协规格管理信息(逻辑删)")
    @PostMapping("/deleteAssistSpec/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids) {
        tmAssistSpecService.deleteAssistSpec(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.tm.assistSpec.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<TmAssistSpec> exportData(@RequestBody TmAssistSpec dto) {
        dto.setOrderStr(orderStr());
        List<TmAssistSpec> list = tmAssistSpecService.listAssistSpec(dto);
        return list;
    }

    @Log(title = "ui.tm.assistSpec.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎面外协规格管理")
    public AjaxResult importData(@RequestBody List<TmAssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tmAssistSpecService.importData(list, updateSupport, importLogId);
    }
}
