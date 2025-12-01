package com.zlt.mix.setting.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.util.CollectionUtils;
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
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.FhGlueReturnRate;
import com.zlt.mix.setting.service.FhGlueReturnRateService;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * 返回胶日返回率Controller
 *
 * @author zlt
 * @date 2022-11-28
 */
@RestController
@RequestMapping("/fhGlueRate")
public class FhGlueReturnRateController extends BaseController {
    @Resource
    private FhGlueReturnRateService fhGlueReturnRateService;

    /**
     * 查询返回胶日返回率列表
     */
    @ApiOperation("查询返回胶日返回率列表")
    @PostMapping("/list")
    public TableDataInfo listFhGlueReturnRate(@RequestBody FhGlueReturnRate fhGlueReturnRate) {
        startPage(false);
        fhGlueReturnRate.setOrderStr(orderStr());
        List<FhGlueReturnRate> list = fhGlueReturnRateService.selectFhGlueReturnRateList(fhGlueReturnRate);
        return getDataTable(list);
    }

    @ApiOperation("获取返回胶日返回率详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public FhGlueReturnRate getFhGlueReturnRateInfo(@PathVariable("id") Long id){
        return fhGlueReturnRateService.getById(id);
    }

    @Log(title = "setting.fhGlueRate.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存返回胶日返回率信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveFhGlueReturnRate(@RequestBody FhGlueReturnRate fhGlueReturnRate) {
        fhGlueReturnRateService.saveFhGlueReturnRate(fhGlueReturnRate);
        return AjaxResult.success();
    }

    @Log(title = "setting.fhGlueRate.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除返回胶日返回率")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteFhGlueReturnRate(@PathVariable Long[] ids){
        return toAjax(fhGlueReturnRateService.deleteFhGlueReturnRateByIds(ids));
    }

    @Log(title = "setting.fhGlueRate.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出返回胶日返回率列表")
    @PostMapping("/exportData")
    public List<FhGlueReturnRate> exportData(@RequestBody FhGlueReturnRate fhGlueReturnRate){
        startPage(false);
        fhGlueReturnRate.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  fhGlueReturnRateService.selectFhGlueReturnRateList(fhGlueReturnRate);
    }

    @ApiOperation("校验返回胶日返回率唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkFhGlueReturnRateUnique")
    public String checkFhGlueReturnRateUnique(@RequestBody FhGlueReturnRate fhGlueReturnRate){
        return fhGlueReturnRateService.checkFhGlueReturnRateUnique(fhGlueReturnRate);
    }

    @Log(title = "setting.fhGlueRate.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入返回胶日返回率数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<FhGlueReturnRate> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return fhGlueReturnRateService.importData(list, updateSupport, importLogId);
    }
}
