package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.MesBasMaterial;
import com.zlt.mix.setting.service.MesBasMaterialService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 物料Controller
 *
 * @author Joran.zhang
 * @date 2022-05-30
 */
@RestController
@RequestMapping("/material")
public class MesBasMaterialController extends BaseController {
    @Resource
    private MesBasMaterialService mesBasMaterialService;

    /**
     * 查询物料列表
     */
    @ApiOperation("查询物料列表")
    @PostMapping("/list")
    public TableDataInfo listMesBasMaterial(@RequestBody MesBasMaterial mesBasMaterial) {
        String glueType=mesBasMaterial.getGlueType();
        //特殊处理空查询
        if("NULL".equals(glueType)){
            mesBasMaterial.setGlueTypeEmpty(glueType);
            mesBasMaterial.setGlueType("");
        }

        startPage(false);
        mesBasMaterial.setOrderStr(orderStr());
        List<MesBasMaterial> list = mesBasMaterialService.selectMesBasMaterialList(mesBasMaterial);
        return getDataTable(list);
    }

    @ApiOperation("获取物料详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public MesBasMaterial getMesBasMaterialInfo(@PathVariable("id") Long id){
        return mesBasMaterialService.getById(id);
    }

    @Log(title = "setting.material.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存物料信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveMesBasMaterial(@RequestBody MesBasMaterial mesBasMaterial) {
        //mesBasMaterialService.saveMesBasMaterial(mesBasMaterial);
        MesBasMaterial updateData=new MesBasMaterial();
        updateData.setId(mesBasMaterial.getId());
        updateData.setGlueType(mesBasMaterial.getGlueType());
        updateData.setMinparkTime(mesBasMaterial.getMinparkTime());
        updateData.setRemark(mesBasMaterial.getRemark());
        mesBasMaterialService.updateById(updateData);
        return AjaxResult.success();
    }

    @Log(title = "setting.material.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除物料")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteMesBasMaterial(@PathVariable Long[] ids){
        return toAjax(mesBasMaterialService.deleteMesBasMaterialByIds(ids));
    }

    @Log(title = "setting.material.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出物料列表")
    @PostMapping("/exportData")
    public List<MesBasMaterial> exportData(@RequestBody MesBasMaterial mesBasMaterial){
        startPage(false);
        mesBasMaterial.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  mesBasMaterialService.selectMesBasMaterialList(mesBasMaterial);
    }

    @ApiOperation("校验物料唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkMesBasMaterialUnique")
    public String checkMesBasMaterialUnique(@RequestBody MesBasMaterial mesBasMaterial){
        return mesBasMaterialService.checkMesBasMaterialUnique(mesBasMaterial);
    }

    @Log(title = "setting.material.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入物料数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MesBasMaterial> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return mesBasMaterialService.importData(list, updateSupport, importLogId);
    }

    /**
     * 汇总胶料需求计划选机台
     */
    @PostMapping("/chooseGlue")
    public AjaxResult chooseGlue(@RequestBody MesBasMaterial mesBasMaterial) {
        mesBasMaterialService.updateById(mesBasMaterial);
        return AjaxResult.success();
    }

    /**
     * 根据物料大类列表查询物料名称列表
     */
    @ApiOperation("根据物料大类列表查询物料名称列表")
    @PostMapping("/listMaterialName")
    public List<String> listMesBasMaterial(@RequestBody List<Integer> majorTypes) {
        return mesBasMaterialService.listMesBasMaterial(majorTypes);
    }
}
