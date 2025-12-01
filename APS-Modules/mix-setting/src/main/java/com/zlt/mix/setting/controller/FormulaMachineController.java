package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.FormulaMachine;
import com.zlt.mix.setting.service.FormulaMachineService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 配方与机台对应Controller
 *
 * @author Gim
 * @date 2022-03-28
 */
@RestController
@RequestMapping("/formulaMachine")
public class FormulaMachineController extends BaseController {
    @Resource
    private FormulaMachineService formulaMachineService;

    /**
     * 查询配方与机台对应列表
     */
    @ApiOperation("查询配方与机台对应列表")
    @PostMapping("/list")
    public TableDataInfo listFormulaMachine(@RequestBody FormulaMachine formulaMachine) {
        startPage(false);
        formulaMachine.setOrderStr(orderStr());
        List<FormulaMachine> list = formulaMachineService.selectFormulaMachineList(formulaMachine);
        return getDataTable(list);
    }

    @ApiOperation("获取配方与机台对应详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public FormulaMachine getFormulaMachineInfo(@PathVariable("id") Long id) {
        FormulaMachine machine = formulaMachineService.getById(id);

        List<FormulaMachine> list = formulaMachineService.selectExactFormulaMachineList(machine);
        if (CollectionUtils.isEmpty(list)) {
            return new FormulaMachine();
        }
        return list.get(0);
    }

    @ApiOperation("获取配方与机台对应详细信息")
    @GetMapping(value = "/{mixArea}/{glue}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mixArea", dataType = "String", value = "密炼区", paramType = "query"),
            @ApiImplicitParam(name = "glue", dataType = "String", value = "胶料号", paramType = "query")
    })
    public FormulaMachine getFormulaMachineInfo(@PathVariable("mixArea") String mixArea, @PathVariable("glue") String glue) {
        FormulaMachine machine = new FormulaMachine();
        machine.setMixArea(mixArea);
        machine.setGlue(glue);
        List<FormulaMachine> list = formulaMachineService.selectExactFormulaMachineList(machine);
        if (CollectionUtils.isEmpty(list)) {
            return new FormulaMachine();
        }
        return list.get(0);
    }

    @Log(title = "setting.formulaMachine.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存配方与机台对应信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveFormulaMachine(@RequestBody FormulaMachine formulaMachine) {
        Long id = formulaMachine.getId();
        formulaMachineService.saveFormulaMachine(formulaMachine);
        //返回ID和机台名称进行显示
        if (id != null) {
            FormulaMachine formulaMachineInfo = getFormulaMachineInfo(formulaMachine.getMixArea(), formulaMachine.getGlue());
            return AjaxResult.success(I18nUtil.getMessage("common.msg.ajax.operation.success"), formulaMachineInfo);
        }
        return AjaxResult.success();
    }

    @Log(title = "setting.formulaMachine.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除配方与机台对应")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteFormulaMachine(@PathVariable Long[] ids) {
        return toAjax(formulaMachineService.deleteFormulaMachineByIds(ids));
    }

    @Log(title = "setting.formulaMachine.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出配方与机台对应列表")
    @PostMapping("/exportData")
    public List<FormulaMachine> exportData(@RequestBody FormulaMachine formulaMachine) {
        startPage(false);
        formulaMachine.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return formulaMachineService.selectFormulaMachineList(formulaMachine);
    }

    @ApiOperation("校验配方与机台对应唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkFormulaMachineUnique")
    public String checkFormulaMachineUnique(@RequestBody FormulaMachine formulaMachine) {
        return formulaMachineService.checkFormulaMachineUnique(formulaMachine);
    }

    @Log(title = "setting.formulaMachine.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入配方与机台对应数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<FormulaMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return formulaMachineService.importData(list, updateSupport, importLogId);
    }

    /**
     * 根据密炼区和胶料名称进行精确查询
     * @param formulaMachine 机台名称、胶料名称
     * @return 查询出来的集合
     */
    @ApiOperation("根据密炼区和胶料名称进行精确查询")
    @PostMapping("/getFormulaMachineList")
    public ArrayList<FormulaMachine> getFormulaMachineList(@RequestBody FormulaMachine formulaMachine) {
        return formulaMachineService.getFormulaMachineList(formulaMachine);
    }


    /**
     * 根据密炼区和胶料名称查询配方机台列表
     * @param formulaMachine 密炼区、胶料名称
     * @return 查询出来的集合
     */
    @ApiOperation("根据密炼区和胶料名称进行精确查询")
    @PostMapping("/getRecipeMachineList")
    public ArrayList<FormulaMachine> getRecipeMachineList(@RequestBody FormulaMachine formulaMachine) {
        return formulaMachineService.selectRecipeMachineList(formulaMachine);
    }
    
}
