package com.zlt.mix.setting.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.mix.setting.api.domain.entity.LhflSpanSetting;
import com.zlt.mix.setting.service.LhflSpanSettingService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.common.core.utils.ExcelUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 硫磺辅料跨区设置Controller
 *
 * @author chen
 * @date 2022-08-12
 */
@RestController
@RequestMapping("/lhflSpanSetting")
public class LhflSpanSettingController extends BaseController {
    @Resource
    private LhflSpanSettingService lhflSpanSettingService;

    /**
     * 查询硫磺辅料跨区设置列表
     */
    @ApiOperation("查询硫磺辅料跨区设置列表")
    @PostMapping("/list")
    public TableDataInfo listLhflSpanSetting(@RequestBody LhflSpanSetting lhflSpanSetting) {
        startPage(false);
        lhflSpanSetting.setOrderStr(orderStr());
        List<LhflSpanSetting> list = lhflSpanSettingService.selectLhflSpanSettingList(lhflSpanSetting);
        return getDataTable(list);
    }

    @ApiOperation("获取硫磺辅料跨区设置详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public LhflSpanSetting getLhflSpanSettingInfo(@PathVariable("id") Long id){
        return lhflSpanSettingService.getById(id);
    }

    @Log(title = "setting.lhflSpanSetting.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存硫磺辅料跨区设置信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveLhflSpanSetting(@RequestBody LhflSpanSetting lhflSpanSetting) {
        lhflSpanSettingService.saveLhflSpanSetting(lhflSpanSetting);
        return AjaxResult.success();
    }

    @Log(title = "setting.lhflSpanSetting.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除硫磺辅料跨区设置")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteLhflSpanSetting(@PathVariable Long[] ids){
        return toAjax(lhflSpanSettingService.deleteLhflSpanSettingByIds(ids));
    }

    @Log(title = "setting.lhflSpanSetting.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出硫磺辅料跨区设置列表")
    @PostMapping("/exportData")
    public List<LhflSpanSetting> exportData(@RequestBody LhflSpanSetting lhflSpanSetting){
        startPage(false);
        lhflSpanSetting.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  lhflSpanSettingService.selectLhflSpanSettingList(lhflSpanSetting);
    }

    @ApiOperation("校验硫磺辅料跨区设置唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkLhflSpanSettingUnique")
    public String checkLhflSpanSettingUnique(@RequestBody LhflSpanSetting lhflSpanSetting){
        return lhflSpanSettingService.checkLhflSpanSettingUnique(lhflSpanSetting);
    }

    @Log(title = "setting.lhflSpanSetting.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入硫磺辅料跨区设置数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhflSpanSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return lhflSpanSettingService.importData(list, updateSupport, importLogId);
    }
}
