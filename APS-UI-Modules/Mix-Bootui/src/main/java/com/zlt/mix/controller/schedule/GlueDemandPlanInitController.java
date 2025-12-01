package com.zlt.mix.controller.schedule;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;
import com.zlt.mix.schedule.api.service.IGlueDemandPlanInitService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * 分厂胶料需求计划（初始表）Controller
 * @author Gim
 * @date 2022-04-05
 */
@Api(tags = "分厂胶料需求计划（初始表）")
@Controller
@RequestMapping("/schedule/factoryGluePlanStatistics")
public class GlueDemandPlanInitController extends BaseController {

    @Resource
    private IGlueDemandPlanInitService iGlueDemandPlanInitService;
    @Resource
    private IExportLogService iExportLogService;

    private final String prefix = "schedule/factoryGluePlanStatistics";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("schedule:factoryGluePlanStatistics:view")
    @GetMapping()
    public String toIndex(ModelMap modelMap) {
        modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/factoryGluePlanStatistics";
    }

    @ApiOperation("根据条件查询分厂胶料需求计划（初始表）列表")
    @RequiresPermissions("schedule:factoryGluePlanStatistics:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listGlueDemandPlanInit(GlueDemandPlanInit entity) {
        return iGlueDemandPlanInitService.listGlueDemandPlanInit(entity);
    }


    /**
     * 导出分厂胶料需求计划（初始表）
     */
    @ApiOperation("导出分厂胶料需求计划（初始表）")
    @RequiresPermissions("schedule:factoryGluePlanStatistics:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,GlueDemandPlanInit glueDemandPlanInit) throws IOException {
        //若是没传日期则默认查询当日
        if (glueDemandPlanInit.getPlanDate() == null) {
            glueDemandPlanInit.setPlanDate(DateUtils.getNowDate());
        }
        //获取字节流数据
        byte[] data = iGlueDemandPlanInitService.exportData(glueDemandPlanInit);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("schedule.factoryGluePlanStatistics.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, glueDemandPlanInit.toString(), ZltConstant.PROCEDURE_CODE_MIX);
        iExportLogService.add(exportLog);
    }


}
