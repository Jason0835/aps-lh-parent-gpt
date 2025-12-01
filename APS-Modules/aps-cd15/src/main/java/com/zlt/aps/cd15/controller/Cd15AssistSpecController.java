package com.zlt.aps.cd15.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15AssistSpec;
import com.zlt.aps.cd15.service.Cd15AssistSpecService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"15度裁断外协规格管理接口"})
@RestController
@RequestMapping("/cd15/assistSpec")
public class Cd15AssistSpecController extends BaseController {

    @Resource
    private Cd15AssistSpecService cd15AssistSpecService;

    @ApiOperation("根据条件查询外协规格管理列表")
    @PostMapping("/listAssistSpec")
    public TableDataInfo listAssistSpec(@RequestBody Cd15AssistSpec dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd15AssistSpec> list = cd15AssistSpecService.listAssistSpec(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询外协规格管理信息")
    @GetMapping("/getAssistSpec/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd15AssistSpec getAssistSpec(@PathVariable("id") Long id) {
        Cd15AssistSpec dto = new Cd15AssistSpec();
        BeanUtils.copyProperties(cd15AssistSpecService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.cd15.assistSpec.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存外协规格管理信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveAssistSpec")
    public AjaxResult saveAssistSpec(@RequestBody Cd15AssistSpec dto) {
        Cd15AssistSpec entity = new Cd15AssistSpec();
        BeanUtils.copyProperties(dto, entity);
        cd15AssistSpecService.saveAssistSpec(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断钢压大卷代号是否已经存在")
    @PostMapping("/checkAssistSpecCodeUnique")
    public String checkAssistSpecCodeUnique(@RequestBody Cd15AssistSpec dto) {
        return cd15AssistSpecService.checkAssistSpecCodeUnique(dto);
    }

    @Log(title = "ui.cd15.assistSpec.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除外协规格管理信息(逻辑删)")
    @PostMapping("/deleteAssistSpec/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids) {
        cd15AssistSpecService.deleteAssistSpec(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.cd15.assistSpec.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<Cd15AssistSpec> exportData(@RequestBody Cd15AssistSpec dto) {
        dto.setOrderStr(orderStr());
        List<Cd15AssistSpec> list = cd15AssistSpecService.listAssistSpec(dto);
        return list;
    }

    @Log(title = "ui.cd15.assistSpec.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入15度裁断外协规格管理")
    public AjaxResult importData(@RequestBody List<Cd15AssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cd15AssistSpecService.importData(list, updateSupport, importLogId);
    }
}
