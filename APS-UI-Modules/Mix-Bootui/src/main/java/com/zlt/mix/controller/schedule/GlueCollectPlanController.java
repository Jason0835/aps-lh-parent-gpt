package com.zlt.mix.controller.schedule;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.schedule.api.domain.dto.GlueCollectPlanExportDictDto;
import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;
import com.zlt.mix.schedule.api.service.IGlueCollectPlanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 汇总胶料需求计划Controller
 *
 * @author chen
 * @date 2022-04-25
 */
@Api(tags = "汇总胶料需求计划")
@Controller
@RequestMapping("/schedule/glueCollectPlan")
public class GlueCollectPlanController extends BaseController {

    @Resource
    private IGlueCollectPlanService iGlueCollectPlanService;
    @Resource
    private IExportLogService iExportLogService;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    private final String prefix = "schedule/glueCollectPlan";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("schedule:glueCollectPlan:view")
    @GetMapping()
    public String toIndex(ModelMap modelMap) {
        modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/glueCollectPlan";
    }

    /**
     * 跳转至汇总计划页面
     */
    @GetMapping("/toSummaryPlan")
    public String toSummaryPlan(ModelMap modelMap) {
        modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/summaryPlan";
    }

    /**
     * 跳转至选机台页面
     */
    @GetMapping("/toChooseMachine/{id}")
    public String toChooseMachine(@PathVariable("id") Long id, ModelMap modelMap) {
        modelMap.put("glueCollectPlan", iGlueCollectPlanService.getGlueCollectPlanInfo(id));
        return prefix + "/chooseMachine";
    }

    @ApiOperation("根据条件查询汇总胶料需求计划列表")
    @RequiresPermissions("schedule:glueCollectPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listGlueCollectPlan(GlueCollectPlan glueCollectPlan) {
        return iGlueCollectPlanService.listGlueCollectPlan(glueCollectPlan);
    }

    @ApiOperation("修改或新增汇总胶料需求计划")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueCollectPlan(GlueCollectPlan glueCollectPlan) {
        return iGlueCollectPlanService.saveGlueCollectPlan(glueCollectPlan);
    }

    @ApiOperation("删除汇总胶料需求计划（id不为空）")
    @RequiresPermissions("schedule:glueCollectPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeGlueCollectPlan(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGlueCollectPlanService.deleteGlueCollectPlan(arr);
    }

    /**
     * 导出汇总胶料需求计划
     */
    @ApiOperation("导出汇总胶料需求计划")
    @RequiresPermissions("schedule:glueCollectPlan:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GlueCollectPlan glueCollectPlan) throws IOException {
        String fileName = I18nUtil.getMessage("schedule.glueCollectPlan.modelName");
        GlueCollectPlanExportDictDto dictDto = new GlueCollectPlanExportDictDto();
        HashMap<String, String> factoryDictMap = iSysDictDataCacheService.getType("FACTORY").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        HashMap<String, String> mixAreaDictMap = iSysDictDataCacheService.getType("MIX_AREA").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        dictDto.setFactoryDictMap(factoryDictMap);
        dictDto.setMixAreaDictMap(mixAreaDictMap);
        BeanUtils.copyProperties(glueCollectPlan, dictDto);
        byte[] data = iGlueCollectPlanService.exportData(dictDto);
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dictDto.toString(), ZltConstant.PROCEDURE_CODE_MIX);
        iExportLogService.add(exportLog);
    }

    /**
     * 汇总胶料需求计划选机台
     */
    @ApiOperation("汇总胶料需求计划选机台")
    @RequiresPermissions("schedule:glueCollectPlan:chooseMachine")
    @PostMapping("/chooseMachine")
    @ResponseBody
    public AjaxResult chooseMachine(GlueCollectPlan glueCollectPlan) {
        return iGlueCollectPlanService.chooseMachine(glueCollectPlan);
    }

    /**
     * 汇总胶料需求计划
     */
    @ApiOperation("汇总胶料需求计划")
    @RequiresPermissions("schedule:glueCollectPlan:summaryPlan")
    @PostMapping("/summaryPlan")
    @ResponseBody
    public AjaxResult summaryPlan(GlueCollectPlan glueCollectPlan) {
        if (glueCollectPlan.getPlanDate() == null) {
            glueCollectPlan.setPlanDate(DateUtils.addDays(new Date(), 1));
        }
        return iGlueCollectPlanService.summaryPlan(glueCollectPlan);
    }

    @ApiOperation("检测对应日期的数据是否存在")
    @PostMapping("/checkPlanDateExist")
    @ResponseBody
    public AjaxResult checkPlanDateExist(GlueCollectPlan glueCollectPlan) {
        String unique = iGlueCollectPlanService.checkPlanDateExist(glueCollectPlan);

        //避免ZltConstant是否唯一的常量值修改，在此处定义0为唯一，1为不唯一
        if (ZltConstant.UNIQUE.equals(unique)) {
            return AjaxResult.success("0");
        }
        if (ZltConstant.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.success("1");
        }
        return AjaxResult.error();
    }
}
