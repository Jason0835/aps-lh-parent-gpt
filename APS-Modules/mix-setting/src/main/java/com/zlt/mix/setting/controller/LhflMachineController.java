package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import com.zlt.mix.setting.service.LhflMachineService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 小料机台信息Controller
 *
 * @author Liam
 * @date 2022-04-18
 */
@RestController
@RequestMapping("/lhflMachine")
public class LhflMachineController extends BaseController {
    @Resource
    private LhflMachineService lhflMachineService;

    /**
     * 查询小料机台信息列表
     */
    @ApiOperation("查询小料机台信息列表")
    @PostMapping("/list")
    public TableDataInfo listLhflMachine(@RequestBody LhflMachine lhflMachine) {
        startPage(false);
        lhflMachine.setOrderStr(orderStr());
        List<LhflMachine> list = lhflMachineService.selectLhflMachineList(lhflMachine);
        return getDataTable(list);
    }

    @ApiOperation("获取小料机台信息详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public LhflMachine getLhflMachineInfo(@PathVariable("id") Long id) {
        return lhflMachineService.getById(id);
    }

    @Log(title = "setting.lhflMachine.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存小料机台信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveLhflMachine(@RequestBody LhflMachine lhflMachine) {
        lhflMachineService.saveLhflMachine(lhflMachine);
        return AjaxResult.success();
    }

    @Log(title = "setting.lhflMachine.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除小料机台信息")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteLhflMachine(@PathVariable Long[] ids) {
        return toAjax(lhflMachineService.deleteLhflMachineByIds(ids));
    }

    @Log(title = "setting.lhflMachine.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出小料机台信息列表")
    @PostMapping("/exportData")
    public List<LhflMachine> exportData(@RequestBody LhflMachine lhflMachine) {
        startPage(false);
        lhflMachine.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return lhflMachineService.selectLhflMachineList(lhflMachine);
    }

    @ApiOperation("校验小料机台信息唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkLhflMachineUnique")
    public String checkLhflMachineUnique(@RequestBody LhflMachine lhflMachine) {
        String unique = lhflMachineService.checkLhflMachineUnique(lhflMachine);
        if (ZltConstant.NOT_UNIQUE.equals(unique)) {
            return unique;
        }
        return lhflMachineService.checkLhflMachineUnique2(lhflMachine);
    }

    @Log(title = "setting.lhflMachine.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入小料机台信息数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhflMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return lhflMachineService.importData(list, updateSupport, importLogId);
    }
}
