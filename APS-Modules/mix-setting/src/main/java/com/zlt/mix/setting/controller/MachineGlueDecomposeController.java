package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.dto.MachineGlueDecomposeDto;
import com.zlt.mix.setting.api.domain.entity.MachineGlueDecompose;
import com.zlt.mix.setting.service.MachineGlueDecomposeService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 密炼机指定胶料分解Controller
 *
 * @author Liam
 * @date 2022-03-29
 */
@RestController
@RequestMapping("/machineGlueDecompose")
public class MachineGlueDecomposeController extends BaseController {
    @Resource
    private MachineGlueDecomposeService machineGlueDecomposeService;

    /**
     * 查询密炼机指定胶料分解列表(级联查询机台名称)
     */
    @ApiOperation("查询密炼机指定胶料分解列表(级联查询机台名称)")
    @PostMapping("/list")
    public TableDataInfo listMachineGlueDecompose(@RequestBody MachineGlueDecompose machineGlueDecompose) {
        startPage(false);
        machineGlueDecompose.setOrderStr(orderStr());
        List<MachineGlueDecomposeDto> list = machineGlueDecomposeService.selectMachineGlueDecomposeListCascade(machineGlueDecompose);
        return getDataTable(list);
    }


    @ApiOperation("获取密炼机指定胶料分解详细信息(级联查询机台名称)")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public MachineGlueDecomposeDto getMachineGlueDecomposeInfo(@PathVariable("id") Long id) {
        return machineGlueDecomposeService.getByIdCascade(id);
    }

    @Log(title = "setting.machineGlueDecompose.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存密炼机指定胶料分解信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveMachineGlueDecompose(@RequestBody MachineGlueDecompose machineGlueDecompose) {
        machineGlueDecomposeService.saveMachineGlueDecompose(machineGlueDecompose);
        return AjaxResult.success();
    }

    @Log(title = "setting.machineGlueDecompose.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除密炼机指定胶料分解")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteMachineGlueDecompose(@PathVariable Long[] ids) {
        return toAjax(machineGlueDecomposeService.deleteMachineGlueDecomposeByIds(ids));
    }

    @Log(title = "setting.machineGlueDecompose.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出密炼机指定胶料分解列表(级联查询机台名称)")
    @PostMapping("/exportData")
    public List<MachineGlueDecomposeDto> exportData(@RequestBody MachineGlueDecompose machineGlueDecompose) {
        startPage();
        machineGlueDecompose.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return machineGlueDecomposeService.selectMachineGlueDecomposeListCascade(machineGlueDecompose);
    }

    @ApiOperation("校验密炼机指定胶料分解唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkMachineGlueDecomposeUnique")
    public String checkMachineGlueDecomposeUnique(@RequestBody MachineGlueDecompose machineGlueDecompose) {
        return machineGlueDecomposeService.checkMachineGlueDecomposeUnique(machineGlueDecompose);
    }

    @Log(title = "setting.machineGlueDecompose.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入密炼机指定胶料分解数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MachineGlueDecomposeDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return machineGlueDecomposeService.importData(list, updateSupport, importLogId);
    }
}
