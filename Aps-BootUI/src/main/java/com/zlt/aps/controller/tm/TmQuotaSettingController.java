package com.zlt.aps.controller.tm;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.tm.TmQuotaSettingTemp;
import com.zlt.aps.tm.api.domain.entity.TmQuotaSetting;
import com.zlt.aps.tm.api.service.ITmQuotaSettingService;
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
import java.util.List;

/**
 * 胎面定额设定Controller
 *
 * @author zlt
 * @date 2021-06-28
 */
@Api(tags = "胎面定额设定")
@Controller
@RequestMapping("/tm/quota")
public class TmQuotaSettingController extends BaseController {

    private final String prefix = "tm/quota";
    @Autowired
    private ITmQuotaSettingService iTmQuotaSettingService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tm:quota:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/quota";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tmQuotaSetting", new TmQuotaSetting());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tmQuotaSetting", iTmQuotaSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询胎面定额设定列表
     */
    @ApiOperation("根据条件查询胎面定额设定列表")
    @RequiresPermissions("tm:quota:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmQuotaSetting entity) {
        return iTmQuotaSettingService.list(entity);
    }

    /**
     * 修改或新增胎面定额设定
     */
    @ApiOperation("修改或新增胎面定额设定")
    @RequiresPermissions("tm:quota:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TmQuotaSetting tmQuotaSetting) {
        AjaxResult ajaxResult = null;
        String unique = iTmQuotaSettingService.checkTmQuotaSettingUnique(tmQuotaSetting);
        if (UserConstants.UNIQUE.equals(unique)) {
            if (tmQuotaSetting.getId() != null) {
                ajaxResult = iTmQuotaSettingService.edit(tmQuotaSetting);
            } else {
                ajaxResult = iTmQuotaSettingService.add(tmQuotaSetting);
            }
        } else {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.quota.uniqueError"));
        }
        return ajaxResult;
    }

    /**
     * 删除胎面定额设定
     */
    @ApiOperation("删除胎面定额设定（id不为空）")
    @RequiresPermissions("tm:quota:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTmQuotaSettingService.remove(arr);
    }

    /**
     * 导出胎面定额设定
     */
    @ApiOperation("导出胎面定额设定")
    @RequiresPermissions("tm:quota:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TmQuotaSetting tmQuotaSetting) throws IOException {
        List<TmQuotaSetting> list = iTmQuotaSettingService.getList(tmQuotaSetting);
        ExcelUtil<TmQuotaSetting> util = new ExcelUtil(TmQuotaSetting.class);
        String fileName = I18nUtil.getMessage("ui.tm.quota.export.sheetName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, tmQuotaSetting.toString(), ApsConstant.PROCEDURE_CODE_TM);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.tm.quota.export.fileName");
        ExcelUtil<TmQuotaSettingTemp> util = new ExcelUtil<>(TmQuotaSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("tm:quota:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TM,
                I18nUtil.getMessage("ui.tm.quota.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TmQuotaSetting> util = new ExcelUtil<>(TmQuotaSetting.class);
        List<TmQuotaSetting> list = util.importExcel(in);
        AjaxResult ajaxResult = iTmQuotaSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
