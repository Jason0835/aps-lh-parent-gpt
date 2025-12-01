package com.zlt.mix.schedule.controller;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.schedule.api.domain.dto.GlueCollectPlanExportDictDto;
import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;
import com.zlt.mix.schedule.service.GlueCollectPlanService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 汇总胶料需求计划Controller
 *
 * @author chen
 * @date 2022-04-25
 */
@RestController
@RequestMapping("/glueCollectPlan")
public class GlueCollectPlanController extends BaseController {
    @Resource
    private GlueCollectPlanService glueCollectPlanService;

    /**
     * 查询汇总胶料需求计划列表
     */
    @ApiOperation("查询汇总胶料需求计划列表")
    @PostMapping("/list")
    public TableDataInfo listGlueCollectPlan(@RequestBody GlueCollectPlan glueCollectPlan) throws ParseException {
        startPage(false);
        glueCollectPlan.setOrderStr(orderStr());
        if (glueCollectPlan.getPlanDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            glueCollectPlan.setPlanDate(sdf.parse(format));
        }
        List<GlueCollectPlan> list = glueCollectPlanService.selectGlueCollectPlanList(glueCollectPlan);
        return getDataTable(list);
    }

    @ApiOperation("获取汇总胶料需求计划详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueCollectPlan getGlueCollectPlanInfo(@PathVariable("id") Long id) {
        return glueCollectPlanService.getById(id);
    }

    @Log(title = "schedule.glueCollectPlan.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存汇总胶料需求计划信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveGlueCollectPlan(@RequestBody GlueCollectPlan glueCollectPlan) {
        glueCollectPlanService.saveGlueCollectPlan(glueCollectPlan);
        return AjaxResult.success();
    }

    @Log(title = "schedule.glueCollectPlan.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除汇总胶料需求计划")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueCollectPlan(@PathVariable Long[] ids) {
        return toAjax(glueCollectPlanService.deleteGlueCollectPlanByIds(ids));
    }

    @Log(title = "schedule.glueCollectPlan.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出汇总胶料需求计划列表")
    @PostMapping("/exportData")
    public byte[] exportData(@RequestBody GlueCollectPlanExportDictDto dto) throws ParseException {
        startPage(false);
        dto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        if (dto.getPlanDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            dto.setPlanDate(sdf.parse(format));
        }
        return glueCollectPlanService.exportData(dto);
    }

    /**
     * 汇总胶料计划
     */
    @Log(title = "schedule.glueCollectPlan.modelName", newBusinessType = BusinessConstant.AGGREGATE)
    @ApiOperation("汇总胶料计划")
    @PostMapping("/summaryPlan")
    public AjaxResult summaryPlan(@RequestBody GlueCollectPlan glueCollectPlan){
        String validateMsg=glueCollectPlanService.validateMixAreaData(glueCollectPlan);
            if(StringUtils.isNotEmpty(validateMsg)){
            return AjaxResult.error(validateMsg);
        }
        glueCollectPlanService.summaryPlan(glueCollectPlan);
        return AjaxResult.success();
    }

    /**
     * 汇总胶料需求计划选机台
     */
    @PostMapping("/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody GlueCollectPlan glueCollectPlan) {
        glueCollectPlanService.updateById(glueCollectPlan);
        return AjaxResult.success();
    }

    @ApiOperation("检测对应日期的数据是否存在")
    @PostMapping("/checkPlanDateExist")
    public String checkPlanDateExist(@RequestBody GlueCollectPlan glueCollectPlan) {
        return glueCollectPlanService.checkPlanDateExist(glueCollectPlan);
    }
}
