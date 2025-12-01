package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.dto.MixingTimeDto;
import com.zlt.mix.setting.api.domain.entity.MixingTime;
import com.zlt.mix.setting.service.MixingTimeService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 炼胶时间信息Controller
 *
 * @author Liam
 * @date 2022-03-31
 */
@RestController
@RequestMapping("/mixingTime")
public class MixingTimeController extends BaseController {
    @Resource
    private MixingTimeService mixingTimeService;

    /**
     * 查询炼胶时间信息列表
     */
    @ApiOperation("查询炼胶时间信息列表")
    @PostMapping("/list")
    public TableDataInfo listMixingTime(@RequestBody MixingTime mixingTime) {
        startPage(false);
        mixingTime.setOrderStr(orderStr());
        List<MixingTimeDto> list = mixingTimeService.selectMixingTimeList(mixingTime);
        return getDataTable(list);
    }

    @ApiOperation("获取炼胶时间信息详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public MixingTime getMixingTimeInfo(@PathVariable("id") Long id) {
        return mixingTimeService.getById(id);
    }

    @Log(title = "setting.mixingTime.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存炼胶时间信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveMixingTime(@RequestBody MixingTime mixingTime) {
        mixingTimeService.saveMixingTime(mixingTime);
        return AjaxResult.success();
    }

    @Log(title = "setting.mixingTime.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除炼胶时间信息")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteMixingTime(@PathVariable Long[] ids) {
        return toAjax(mixingTimeService.deleteMixingTimeByIds(ids));
    }

    @Log(title = "setting.mixingTime.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出炼胶时间信息列表")
    @PostMapping("/exportData")
    public List<MixingTimeDto> exportData(@RequestBody MixingTime mixingTime) {
        startPage(false);
        mixingTime.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return mixingTimeService.selectMixingTimeList(mixingTime);
    }

    @ApiOperation("校验炼胶时间信息唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkMixingTimeUnique")
    public String checkMixingTimeUnique(@RequestBody MixingTime mixingTime) {
        return mixingTimeService.checkMixingTimeUnique(mixingTime);
    }

    @Log(title = "setting.mixingTime.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入炼胶时间信息数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixingTimeDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return mixingTimeService.importData(list, updateSupport, importLogId);
    }
}
