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
import com.zlt.aps.lh.api.domain.entity.LhInProductionSpec;
import com.zlt.aps.lh.api.service.ILhInProductionSpecService;
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
 * 硫化机台当前生产规格Controller
 *
 * @author chen
 * @date 2022-03-23
 */
@Api(tags = "硫化机台当前生产规格")
@Controller
@RequestMapping("/lh/inProductionSpec")
public class LhInProductionSpecController extends BaseController {

    @Autowired
    private ILhInProductionSpecService iLhInProductionSpecService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "lh/inProductionSpec";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("lh:inProductionSpec:view")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/inProductionSpec";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("lhInProductionSpec", new LhInProductionSpec());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhInProductionSpec", iLhInProductionSpecService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询硫化机台当前生产规格列表
     */
    @ApiOperation("根据条件查询硫化机台当前生产规格列表")
    @RequiresPermissions("lh:inProductionSpec:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhInProductionSpec entity) {
        return iLhInProductionSpecService.list(entity);
    }

    /**
     * 修改或新增硫化机台当前生产规格
     */
    @ApiOperation("修改或新增硫化机台当前生产规格")
    @RequiresPermissions("lh:inProductionSpec:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(LhInProductionSpec lhInProductionSpec) {
        AjaxResult ajaxResult = null;
        if (lhInProductionSpec.getId() != null) {
            ajaxResult = iLhInProductionSpecService.edit(lhInProductionSpec);
        } else {
            ajaxResult = iLhInProductionSpecService.add(lhInProductionSpec);
        }
        return ajaxResult;
    }

    /**
     * 删除硫化机台当前生产规格
     */
    @ApiOperation("删除硫化机台当前生产规格（id不为空）")
    @RequiresPermissions("lh:inProductionSpec:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhInProductionSpecService.remove(arr);
    }

    /**
     * 校验硫化机台当前生产规格唯一性
     */
    @ApiOperation("校验硫化机台当前生产规格唯一性")
    @PostMapping("/checkLhInProductionSpecUnique")
    @ResponseBody
    public String checkLhInProductionSpecUnique(LhInProductionSpec lhInProductionSpec) {
        return iLhInProductionSpecService.checkLhInProductionSpecUnique(lhInProductionSpec);
    }

    /**
     * 导出硫化机台当前生产规格
     */
    @ApiOperation("导出硫化机台当前生产规格")
    @RequiresPermissions("lh:inProductionSpec:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, LhInProductionSpec lhInProductionSpec) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.inProductionSpec.modelName");
        List<LhInProductionSpec> list = iLhInProductionSpecService.getList(lhInProductionSpec);
        ExcelUtil<LhInProductionSpec> util = new ExcelUtil<>(LhInProductionSpec.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhInProductionSpec.toString(), "ApsConstant.PROCEDURE_CODE_XXX");
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
        String fileName = I18nUtil.getMessage("ui.data.column.inProductionSpec.modelName");
        ExcelUtil<LhInProductionSpec> util = new ExcelUtil<>(LhInProductionSpec.class);
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
    @RequiresPermissions("lh:inProductionSpec:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, "ApsConstant.PROCEDURE_CODE_XXX",
                I18nUtil.getMessage("ui.data.column.inProductionSpec.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<LhInProductionSpec> util = new ExcelUtil<>(LhInProductionSpec.class);
        InputStream in = new ByteArrayInputStream(data);
        List<LhInProductionSpec> list = util.importExcel(in);
        AjaxResult ajaxResult = iLhInProductionSpecService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
