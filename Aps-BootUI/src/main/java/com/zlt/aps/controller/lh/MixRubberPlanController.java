package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.entity.MixRubberPlan;
import com.zlt.aps.lh.api.service.IMixRubberPlanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 胶料日计划计划Controller
 *
 * @author zlt
 * @date 2021-11-10
 */
@Api(tags = "胶料日计划计划")
@Controller
@RequestMapping("/lh/mix/rubberPlan")
public class MixRubberPlanController extends BaseController {

    private final String prefix = "lh/mix/rubberPlan";
    @Autowired
    private IMixRubberPlanService iMixRubberPlanService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    /**
     * 跳转至主页面
     */
    @GetMapping()
    public String toIndex() {
        return prefix + "/rubberPlan";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mixRubberPlan", new MixRubberPlan());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mixRubberPlan", iMixRubberPlanService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询胶料日计划计划列表
     */
    @ApiOperation("根据条件查询胶料日计划计划列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MixRubberPlan entity) {
        return iMixRubberPlanService.list(entity);
    }

    /**
     * 修改或新增胶料日计划计划
     */
    @ApiOperation("修改或新增胶料日计划计划")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MixRubberPlan mixRubberPlan) {
        AjaxResult ajaxResult = null;
        if (mixRubberPlan.getId() != null) {
            ajaxResult = iMixRubberPlanService.edit(mixRubberPlan);
        } else {
            ajaxResult = iMixRubberPlanService.add(mixRubberPlan);
        }
        return ajaxResult;
    }

    /**
     * 删除胶料日计划计划
     */
    @ApiOperation("删除胶料日计划计划（id不为空）")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMixRubberPlanService.remove(arr);
    }

    /**
     * 校验胶料日计划计划唯一性
     */
    @ApiOperation("校验胶料日计划计划唯一性")
    @PostMapping("/checkMixRubberPlanUnique")
    @ResponseBody
    public String checkMixRubberPlanUnique(MixRubberPlan mixRubberPlan) {
        return iMixRubberPlanService.checkMixRubberPlanUnique(mixRubberPlan);
    }

    /**
     * 导出胶料日计划计划
     */
    @ApiOperation("导出胶料日计划计划")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, MixRubberPlan mixRubberPlan) throws IOException {
        String fileName = I18nUtil.getMessage("ui.lh.rubberPlan.export.fileName");
        List<MixRubberPlan> list = iMixRubberPlanService.getList(mixRubberPlan);
        ExcelUtil<MixRubberPlan> util = new ExcelUtil<>(MixRubberPlan.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, mixRubberPlan.toString(), ApsConstant.PROCEDURE_CODE_LH);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载导入模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.lh.rubberPlan.modelName");
        ExcelUtil<MixRubberPlan> util = new ExcelUtil<>(MixRubberPlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(file, ApsConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.lh.rubberPlan.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<MixRubberPlan> util = new ExcelUtil<>(MixRubberPlan.class);
        List<MixRubberPlan> list = util.importExcel(file.getInputStream());
        AjaxResult ajaxResult = iMixRubberPlanService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
