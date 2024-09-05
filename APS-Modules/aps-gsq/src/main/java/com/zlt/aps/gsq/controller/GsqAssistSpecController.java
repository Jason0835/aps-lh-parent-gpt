package com.zlt.aps.gsq.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gsq.api.domain.entity.GsqAssistSpec;
import com.zlt.aps.gsq.service.GsqAssistSpecService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"钢丝圈外协规格管理接口"})
@RestController
@RequestMapping("/assistSpec")
public class GsqAssistSpecController extends BaseController {

    @Resource
    private GsqAssistSpecService gsqAssistSpecService;

    @ApiOperation("根据条件查询外协规格管理列表")
    @GetMapping("/listAssistSpec")
    public TableDataInfo listAssistSpec(GsqAssistSpec dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<GsqAssistSpec> list = gsqAssistSpecService.listAssistSpec(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询外协规格管理信息")
    @GetMapping("/getAssistSpec/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GsqAssistSpec getAssistSpec(@PathVariable("id") Long id) {
        GsqAssistSpec dto = new GsqAssistSpec();
        BeanUtils.copyProperties(gsqAssistSpecService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.gsq.assistSpec.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存外协规格管理信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveAssistSpec")
    public AjaxResult saveAssistSpec(@RequestBody GsqAssistSpec dto) {
        GsqAssistSpec entity = new GsqAssistSpec();
        BeanUtils.copyProperties(dto, entity);
        gsqAssistSpecService.saveAssistSpec(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断代号是否已经存在")
    @PostMapping("/checkAssistSpecCodeUnique")
    public String checkAssistSpecCodeUnique(@RequestBody GsqAssistSpec dto) {
        return gsqAssistSpecService.checkAssistSpecCodeUnique(dto);
    }

    @Log(title = "ui.gsq.assistSpec.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除外协规格管理信息(逻辑删)")
    @PostMapping("/deleteAssistSpec/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids) {
        gsqAssistSpecService.deleteAssistSpec(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.gsq.assistSpec.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<GsqAssistSpec> exportData(GsqAssistSpec dto) {
        dto.setOrderStr(orderStr());
        List<GsqAssistSpec> list = gsqAssistSpecService.listAssistSpec(dto);
        return list;
    }

    @Log(title = "ui.gsq.assistSpec.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢丝圈外协规格管理")
    public AjaxResult importData(@RequestBody List<GsqAssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gsqAssistSpecService.importData(list, updateSupport, importLogId);
    }
}
