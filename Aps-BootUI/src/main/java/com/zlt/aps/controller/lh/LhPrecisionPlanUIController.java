package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.service.ILhPrecisionPlanRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 硫化精度计划Controller
 *
 * @author APS Team
 */
@Api(tags = "硫化精度计划")
@Controller
@RequestMapping("/schedule/lhPrecisionPlan")
public class LhPrecisionPlanUIController extends BaseController {

    @Resource
    private ILhPrecisionPlanRemoteService lhPrecisionPlanRemoteService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "schedule/lhPrecisionPlan";

    private boolean useFileEncrypt = true;

    @RequiresPermissions("schedule:lhPrecisionPlan:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lhPrecisionPlan";
    }

    @ApiOperation("根据条件查询硫化精度计划列表")
    @RequiresPermissions("schedule:lhPrecisionPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listLhPrecisionPlan(LhPrecisionPlan entity) {
        return lhPrecisionPlanRemoteService.listLhPrecisionPlan(entity);
    }

    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("lhPrecisionPlan", new LhPrecisionPlan());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhPrecisionPlan", lhPrecisionPlanRemoteService.getLhPrecisionPlanInfo(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增硫化精度计划")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveLhPrecisionPlan(LhPrecisionPlan lhPrecisionPlan) {
        return lhPrecisionPlanRemoteService.saveLhPrecisionPlan(lhPrecisionPlan);
    }

    @ApiOperation("删除硫化精度计划（id不为空）")
    @RequiresPermissions("schedule:lhPrecisionPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeLhPrecisionPlan(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return lhPrecisionPlanRemoteService.deleteLhPrecisionPlan(arr);
    }

    @ApiOperation("导出硫化精度计划")
    @RequiresPermissions("schedule:lhPrecisionPlan:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, LhPrecisionPlan lhPrecisionPlan) throws IOException {
        String fileName = I18nUtil.getMessage("ui.lh.precision.plan.model.name");
        List<LhPrecisionPlan> list = lhPrecisionPlanRemoteService.exportData(lhPrecisionPlan);
        ExcelUtil<LhPrecisionPlan> util = new ExcelUtil<>(LhPrecisionPlan.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhPrecisionPlan.toString(), ZltConstant.PROCEDURE_CODE_LH);
        iExportLogService.add(exportLog);
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.lh.precision.plan.model.name");
        ExcelUtil<LhPrecisionPlan> util = new ExcelUtil<>(LhPrecisionPlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("schedule:lhPrecisionPlan:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.lh.precision.plan.model.name"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<LhPrecisionPlan> util = new ExcelUtil<>(LhPrecisionPlan.class);
        List<LhPrecisionPlan> list = util.importExcel(in);
        AjaxResult ajaxResult = lhPrecisionPlanRemoteService.importData(list, updateSupport, importLog.getId());
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    @ApiOperation("从MES同步数据生成硫化精度初版计划")
    @RequiresPermissions("schedule:lhPrecisionPlan:sync")
    @PostMapping("/generateFromMes")
    @ResponseBody
    public AjaxResult generateFromMes() {
        return lhPrecisionPlanRemoteService.generatePlansFromMes();
    }

    @ApiOperation("自动生成年度硫化精度计划")
    @RequiresPermissions("schedule:lhPrecisionPlan:generate")
    @PostMapping("/autoGenerateYearly")
    @ResponseBody
    public AjaxResult autoGenerateYearly(@RequestParam("year") Integer year) {
        return lhPrecisionPlanRemoteService.autoGenerateYearlyPlans(year);
    }

    @ApiOperation("执行30天预警检查")
    @RequiresPermissions("schedule:lhPrecisionPlan:warning")
    @PostMapping("/checkWarning")
    @ResponseBody
    public AjaxResult checkWarning() {
        return lhPrecisionPlanRemoteService.checkWarning();
    }

    @ApiOperation("批量更新到期天数")
    @PostMapping("/batchUpdateDaysToDue")
    @ResponseBody
    public AjaxResult batchUpdateDaysToDue() {
        return lhPrecisionPlanRemoteService.batchUpdateDaysToDue();
    }
}
