package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.LhflScheduleParams;
import com.zlt.mix.setting.service.LhflScheduleParamsService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 排程参数（硫化辅料排程设置）Controller
 *
 * @author Liam
 * @date 2022-04-06
 */
@RestController
@RequestMapping("/lhflScheduleParams")
public class LhflScheduleParamsController extends BaseController {
    @Resource
    private LhflScheduleParamsService lhflScheduleParamsService;

    /**
     * 查询排程参数（硫化辅料排程设置）列表
     */
    @ApiOperation("查询排程参数（硫化辅料排程设置）列表")
    @PostMapping("/list")
    public TableDataInfo listLhflScheduleParams(@RequestBody LhflScheduleParams lhflScheduleParams) {
        startPage(false);
        lhflScheduleParams.setOrderStr(orderStr());
        List<LhflScheduleParams> list = lhflScheduleParamsService.selectLhflScheduleParamsList(lhflScheduleParams);
        return getDataTable(list);
    }

    @ApiOperation("获取排程参数（硫化辅料排程设置）详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public LhflScheduleParams getLhflScheduleParamsInfo(@PathVariable("id") Long id) {
        return lhflScheduleParamsService.getById(id);
    }

    @Log(title = "setting.lhflScheduleParams.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存排程参数（硫化辅料排程设置）信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveLhflScheduleParams(@RequestBody LhflScheduleParams lhflScheduleParams) {
        return lhflScheduleParamsService.saveLhflScheduleParams(lhflScheduleParams);
    }

    @Log(title = "setting.lhflScheduleParams.modelName", newBusinessType = BusinessConstant.INSERT)
    @ApiOperation("复制排程参数（硫化辅料排程设置）信息")
    @PostMapping("/copy")
    public AjaxResult copyLhflScheduleParams(@RequestBody LhflScheduleParams lhflScheduleParams) {
        return lhflScheduleParamsService.copyLhflScheduleParams(lhflScheduleParams);
    }

    @Log(title = "setting.lhflScheduleParams.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出排程参数（硫化辅料排程设置）列表")
    @PostMapping("/exportData")
    public List<LhflScheduleParams> exportData(@RequestBody LhflScheduleParams lhflScheduleParams) {
        startPage(false);
        lhflScheduleParams.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return lhflScheduleParamsService.selectLhflScheduleParamsList(lhflScheduleParams);
    }
}
