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
import com.zlt.aps.cx.api.domain.entity.MidNightShiftFinish;
import com.zlt.aps.cx.api.service.IMidNightShiftFinishService;
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
 * 成型排程中夜班完成量Controller
 * @author chen
 * @date 2022-02-25
 */
@Api(tags = "成型排程中夜班完成量")
@Controller
@RequestMapping("/cx/midNightFinish")
public class MidNightShiftFinishController extends BaseController {

    @Autowired
    private IMidNightShiftFinishService iMidNightShiftFinishService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/midNightFinish";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:midNightFinish:view")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/midNightFinish";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("midNightShiftFinish", new MidNightShiftFinish());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("midNightShiftFinish", iMidNightShiftFinishService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询成型排程中夜班完成量列表
     */
    @ApiOperation("根据条件查询成型排程中夜班完成量列表")
    @RequiresPermissions("cx:midNightFinish:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MidNightShiftFinish entity) {
        return iMidNightShiftFinishService.list(entity);
    }

    /**
     * 修改或新增成型排程中夜班完成量
     */
    @ApiOperation("修改或新增成型排程中夜班完成量")
    @RequiresPermissions("cx:midNightFinish:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MidNightShiftFinish midNightShiftFinish) {
        AjaxResult ajaxResult = null;
        if (midNightShiftFinish.getId() != null){
            ajaxResult = iMidNightShiftFinishService.edit(midNightShiftFinish);
        } else{
            ajaxResult = iMidNightShiftFinishService.add(midNightShiftFinish);
        }
        return ajaxResult;
    }

    /**
     * 删除成型排程中夜班完成量
     */
    @ApiOperation("删除成型排程中夜班完成量（id不为空）")
    @RequiresPermissions("cx:midNightFinish:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMidNightShiftFinishService.remove(arr);
    }

    /**
     * 校验成型排程中夜班完成量唯一性
     */
    @ApiOperation("校验成型排程中夜班完成量唯一性")
    @PostMapping("/checkMidNightShiftFinishUnique")
    @ResponseBody
    public String checkMidNightShiftFinishUnique(MidNightShiftFinish midNightShiftFinish) {
        return iMidNightShiftFinishService.checkMidNightShiftFinishUnique(midNightShiftFinish);
    }

    /**
     * 导出成型排程中夜班完成量
     */
    @ApiOperation("导出成型排程中夜班完成量")
    @RequiresPermissions("cx:midNightFinish:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,MidNightShiftFinish midNightShiftFinish) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.midNightFinish.modelName");
        List<MidNightShiftFinish> list = iMidNightShiftFinishService.getList(midNightShiftFinish);
        ExcelUtil<MidNightShiftFinish> util = new ExcelUtil<>(MidNightShiftFinish. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, midNightShiftFinish.toString(),ApsConstant.PROCEDURE_CODE_CX);
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
        String fileName = I18nUtil.getMessage("ui.data.column.midNightFinish.modelName");
        ExcelUtil<MidNightShiftFinish> util = new ExcelUtil<>(MidNightShiftFinish.class);
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
    @RequiresPermissions("cx:midNightFinish:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.midNightFinish.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<MidNightShiftFinish> util = new ExcelUtil<>(MidNightShiftFinish.class);
        InputStream in = new ByteArrayInputStream(data);
        List<MidNightShiftFinish> list = util.importExcel(in);
        AjaxResult ajaxResult = iMidNightShiftFinishService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
