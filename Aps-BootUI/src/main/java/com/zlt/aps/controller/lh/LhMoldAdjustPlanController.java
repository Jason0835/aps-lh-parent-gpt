package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.entity.LhMoldAdjustPlan;
import com.zlt.aps.lh.api.service.ILhMoldAdjustPlanService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * 硫化模具调整计划Controller
 * @author chen
 * @date 2022-03-23
 */
@Api(tags = "硫化模具调整计划")
@Controller
@RequestMapping("/lh/moldAdjustPlan")
public class LhMoldAdjustPlanController extends BaseController {

    @Autowired
    private ILhMoldAdjustPlanService iLhMoldAdjustPlanService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "lh/moldAdjustPlan";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("lh:moldAdjustPlan:view")
    @GetMapping()
    public String toIndex(ModelMap modelMap) {
        modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));  //当前日期
        return prefix + "/moldAdjustPlan";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("lhMoldAdjustPlan", new LhMoldAdjustPlan());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhMoldAdjustPlan", iLhMoldAdjustPlanService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询硫化模具调整计划列表
     */
    @ApiOperation("根据条件查询硫化模具调整计划列表")
    @RequiresPermissions("lh:moldAdjustPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhMoldAdjustPlan entity) {
        return iLhMoldAdjustPlanService.list(entity);
    }

    /**
     * 修改或新增硫化模具调整计划
     */
    @ApiOperation("修改或新增硫化模具调整计划")
    @RequiresPermissions("lh:moldAdjustPlan:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(LhMoldAdjustPlan lhMoldAdjustPlan) {
        AjaxResult ajaxResult = null;
        if (lhMoldAdjustPlan.getId() != null){
            ajaxResult = iLhMoldAdjustPlanService.edit(lhMoldAdjustPlan);
        } else{
            ajaxResult = iLhMoldAdjustPlanService.add(lhMoldAdjustPlan);
        }
        return ajaxResult;
    }

    /**
     * 删除硫化模具调整计划
     */
    @ApiOperation("删除硫化模具调整计划（id不为空）")
    @RequiresPermissions("lh:moldAdjustPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhMoldAdjustPlanService.remove(arr);
    }

    /**
     * 校验硫化模具调整计划唯一性
     */
    @ApiOperation("校验硫化模具调整计划唯一性")
    @PostMapping("/checkLhMoldAdjustPlanUnique")
    @ResponseBody
    public String checkLhMoldAdjustPlanUnique(LhMoldAdjustPlan lhMoldAdjustPlan) {
        return iLhMoldAdjustPlanService.checkLhMoldAdjustPlanUnique(lhMoldAdjustPlan);
    }

    /**
     * 导出硫化模具调整计划
     */
    @ApiOperation("导出硫化模具调整计划")
    @RequiresPermissions("lh:moldAdjustPlan:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,LhMoldAdjustPlan lhMoldAdjustPlan) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.moldAdjustPlan.modelName");
        List<LhMoldAdjustPlan> list = iLhMoldAdjustPlanService.getList(lhMoldAdjustPlan);
        ExcelUtil<LhMoldAdjustPlan> util = new ExcelUtil<>(LhMoldAdjustPlan. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhMoldAdjustPlan.toString(),"ApsConstant.PROCEDURE_CODE_XXX");
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
        String fileName = I18nUtil.getMessage("ui.data.column.moldAdjustPlan.modelName");
        ExcelUtil<LhMoldAdjustPlan> util = new ExcelUtil<>(LhMoldAdjustPlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("lh:moldAdjustPlan:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, "ApsConstant.PROCEDURE_CODE_XXX",
                I18nUtil.getMessage("ui.data.column.moldAdjustPlan.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<LhMoldAdjustPlan> util = new ExcelUtil<>(LhMoldAdjustPlan.class);
        InputStream in = new ByteArrayInputStream(data);
        List<LhMoldAdjustPlan> list = util.importExcel(in);
        AjaxResult ajaxResult = iLhMoldAdjustPlanService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
