package com.zlt.aps.controller.cx;

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
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.InProductionSpec;
import com.zlt.aps.cx.api.service.IInProductionSpecService;
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
 * 成型机台当前生产规格Controller
 *
 * @author chen
 * @date 2022-02-25
 */
@Api(tags = "成型机台当前生产规格")
@Controller
@RequestMapping("/cx/inProductionSpec")
public class InProductionSpecController extends BaseController {

    @Autowired
    private IInProductionSpecService iInProductionSpecService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/inProductionSpec";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:inProductionSpec:view")
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
        mmap.put("inProductionSpec", new InProductionSpec());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("inProductionSpec", iInProductionSpecService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询成型机台当前生产规格列表
     */
    @ApiOperation("根据条件查询成型机台当前生产规格列表")
    @RequiresPermissions("cx:inProductionSpec:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(InProductionSpec entity) {
        return iInProductionSpecService.list(entity);
    }

    /**
     * 修改或新增成型机台当前生产规格
     */
    @ApiOperation("修改或新增成型机台当前生产规格")
    @RequiresPermissions("cx:inProductionSpec:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(InProductionSpec inProductionSpec) {
        AjaxResult ajaxResult = null;
        if (inProductionSpec.getId() != null) {
            ajaxResult = iInProductionSpecService.edit(inProductionSpec);
        } else {
            ajaxResult = iInProductionSpecService.add(inProductionSpec);
        }
        return ajaxResult;
    }

    /**
     * 删除成型机台当前生产规格
     */
    @ApiOperation("删除成型机台当前生产规格（id不为空）")
    @RequiresPermissions("cx:inProductionSpec:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iInProductionSpecService.remove(arr);
    }

    /**
     * 校验成型机台当前生产规格唯一性
     */
    @ApiOperation("校验成型机台当前生产规格唯一性")
    @PostMapping("/checkInProductionSpecUnique")
    @ResponseBody
    public String checkInProductionSpecUnique(InProductionSpec inProductionSpec) {
        return iInProductionSpecService.checkInProductionSpecUnique(inProductionSpec);
    }

    /**
     * 导出成型机台当前生产规格
     */
    @ApiOperation("导出成型机台当前生产规格")
    @RequiresPermissions("cx:inProductionSpec:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, InProductionSpec inProductionSpec) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.inProductionSpec.modelName");
        List<InProductionSpec> list = iInProductionSpecService.getList(inProductionSpec);
        ExcelUtil<InProductionSpec> util = new ExcelUtil<>(InProductionSpec.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, inProductionSpec.toString(), ApsConstant.PROCEDURE_CODE_CX);
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
        ExcelUtil<InProductionSpec> util = new ExcelUtil<>(InProductionSpec.class);
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
    @RequiresPermissions("cx:inProductionSpec:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.inProductionSpec.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<InProductionSpec> util = new ExcelUtil<>(InProductionSpec.class);
        InputStream in = new ByteArrayInputStream(data);
        List<InProductionSpec> list = util.importExcel(in);
        AjaxResult ajaxResult = iInProductionSpecService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
