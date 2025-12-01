package com.zlt.mix.setting.controller;

import java.util.List;

import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import javax.annotation.Resource;

import com.zlt.mix.setting.api.domain.entity.LhflLossSetting;
import com.zlt.mix.setting.service.LhflLossSettingService;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 硫磺辅料耗损率设定Controller
 *
 * @author Joran.zhang
 * @date 2022-05-23
 */
@RestController
@RequestMapping("/lhflLossSetting")
public class LhflLossSettingController extends BaseController {
    @Resource
    private LhflLossSettingService lhflLossSettingService;

    /**
     * 查询硫磺辅料耗损率设定列表
     */
    @ApiOperation("查询硫磺辅料耗损率设定列表")
    @PostMapping("/list")
    public TableDataInfo listLhflLossSetting(@RequestBody LhflLossSetting lhflLossrateSetting) {
        startPage(false);
        lhflLossrateSetting.setOrderStr(orderStr());
        List<LhflLossSetting> list = lhflLossSettingService.selectLhflLossSettingList(lhflLossrateSetting);
        return getDataTable(list);
    }

    @ApiOperation("获取硫磺辅料耗损率设定详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public LhflLossSetting getLhflLossSettingInfo(@PathVariable("id") Long id){
        return lhflLossSettingService.getById(id);
    }

    @Log(title = "setting.setting.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存硫磺辅料耗损率设定信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveLhflLossSetting(@RequestBody LhflLossSetting lhflLossSetting) {
        lhflLossSettingService.saveLhflLossSetting(lhflLossSetting);
        return AjaxResult.success();
    }

    @Log(title = "setting.setting.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除硫磺辅料耗损率设定")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteLhflLossSetting(@PathVariable Long[] ids){
        return toAjax(lhflLossSettingService.deleteLhflLossSettingByIds(ids));
    }

    @Log(title = "setting.setting.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出硫磺辅料耗损率设定列表")
    @PostMapping("/exportData")
    public List<LhflLossSetting> exportData(@RequestBody LhflLossSetting lhflLossSetting){
        startPage(false);
        lhflLossSetting.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  lhflLossSettingService.selectLhflLossSettingList(lhflLossSetting);
    }

    @ApiOperation("校验硫磺辅料耗损率设定唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkLhflLossSettingUnique")
    public String checkLhflLossSettingUnique(@RequestBody LhflLossSetting lhflLossSetting){
        return lhflLossSettingService.checkLhflLossSettingUnique(lhflLossSetting);
    }

    @Log(title = "setting.setting.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入硫磺辅料耗损率设定数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhflLossSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return lhflLossSettingService.importData(list, updateSupport, importLogId);
    }
}
