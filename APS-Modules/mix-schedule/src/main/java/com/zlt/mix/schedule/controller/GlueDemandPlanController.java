package com.zlt.mix.schedule.controller;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.schedule.api.domain.dto.GlueDemandPlanExportDictDto;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlan;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;
import com.zlt.mix.schedule.service.GlueDemandPlanService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 分厂胶料需求计划Controller
 *
 * @author chen
 * @date 2022-04-18
 */
@RestController
@RequestMapping("/glueDemandPlan")
public class GlueDemandPlanController extends BaseController {
    @Resource
    private GlueDemandPlanService glueDemandPlanService;

    /**
     * 查询分厂胶料需求计划列表
     */
    @ApiOperation("查询分厂胶料需求计划列表")
    @PostMapping("/list")
    public TableDataInfo listGlueDemandPlan(@RequestBody GlueDemandPlan glueDemandPlan) throws ParseException {
        startPage(false);
        glueDemandPlan.setOrderStr(orderStr());
        if (glueDemandPlan.getPlanDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            glueDemandPlan.setPlanDate(sdf.parse(format));
        }
        List<GlueDemandPlan> list = glueDemandPlanService.selectGlueDemandPlanList(glueDemandPlan);
        return getDataTable(list);
    }

    @ApiOperation("获取分厂胶料需求计划详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueDemandPlan getGlueDemandPlanInfo(@PathVariable("id") Long id) {
        return glueDemandPlanService.getById(id);
    }

    @Log(title = "schedule.glueDemandPlan.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存分厂胶料需求计划信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveGlueDemandPlan(@RequestBody GlueDemandPlan glueDemandPlan) {
        glueDemandPlanService.saveGlueDemandPlan(glueDemandPlan);
        return AjaxResult.success();
    }

    @Log(title = "schedule.glueDemandPlan.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除分厂胶料需求计划")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueDemandPlan(@PathVariable Long[] ids) {
        return toAjax(glueDemandPlanService.deleteGlueDemandPlanByIds(ids));
    }

    @Log(title = "schedule.glueDemandPlan.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出分厂胶料需求计划列表")
    @PostMapping("/exportData")
    public byte[] exportData(@RequestBody GlueDemandPlanExportDictDto glueDemandPlan) throws ParseException {
        startPage(false);
        glueDemandPlan.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        if (glueDemandPlan.getPlanDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            glueDemandPlan.setPlanDate(sdf.parse(format));
        }
        return glueDemandPlanService.export(glueDemandPlan);
    }

    @ApiOperation("校验分厂胶料需求计划唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkGlueDemandPlanUnique")
    public String checkGlueDemandPlanUnique(@RequestBody GlueDemandPlan glueDemandPlan) {
        return glueDemandPlanService.checkGlueDemandPlanUnique(glueDemandPlan);
    }

    @Log(title = "schedule.glueDemandPlan.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入分厂胶料需求计划数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
            @ApiImplicitParam(name = "isSkip", dataType = "boolean", value = "是否跳过计划量为0的记录", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<GlueDemandPlanInit> list, @RequestParam("importLogId") Long importLogId, @RequestParam("isSkip") Boolean isSkip) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return glueDemandPlanService.importData(list, importLogId, isSkip);
    }

    /**
     * 拆分需求计划
     * @param list 拆分后的数据
     * @param id 要拆分的数据id
     * @return 结果
     */
    @Log(title = "schedule.glueDemandPlan.modelName", newBusinessType = BusinessConstant.SPLIT)
    @ApiOperation("拆分需求计划")
    @PostMapping("/splitPlan")
    public AjaxResult splitPlan(@RequestBody List<GlueDemandPlan> list, @RequestParam("id") Long id){
        glueDemandPlanService.splitPlan(list, id);
        return AjaxResult.success();
    }

    /**
     * 重新匹配密炼区
     *
     * @param glueDemandPlan 需要重新匹配的计划日期
     * @return 结果
     */
    @Log(title = "schedule.glueDemandPlan.modelName", newBusinessType = BusinessConstant.REMATCH)
    @ApiOperation("重新匹配密炼区")
    @PostMapping("/rematch")
    public AjaxResult rematch(@RequestBody GlueDemandPlan glueDemandPlan) {
        if (glueDemandPlan.getPlanDate() == null) {
            //计划日期不能为空
            throw new RuntimeException(I18nUtil.getMessage("schedule.glueDemandPlan.planDate.empty"));
        }
        glueDemandPlanService.rematch(glueDemandPlan.getPlanDate());
        return AjaxResult.success();
    }

    @ApiOperation("检测对应日期和分厂的数据是否存在")
    @PostMapping("/checkPlanDateAndFactoryExist")
    public String checkPlanDateAndFactoryExist(@RequestBody GlueDemandPlan glueDemandPlan) {
        return glueDemandPlanService.checkPlanDateAndFactoryExist(glueDemandPlan);
    }
}
