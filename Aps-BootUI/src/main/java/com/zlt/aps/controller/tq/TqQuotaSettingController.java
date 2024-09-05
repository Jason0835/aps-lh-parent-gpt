package com.zlt.aps.controller.tq;

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
import com.zlt.aps.template.tq.TqQuotaSettingTemp;
import com.zlt.aps.tq.api.domain.entity.TqQuotaSetting;
import com.zlt.aps.tq.api.service.ITqQuotaSettingService;
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
 * 胎圈定额设定Controller
 *
 * @author zlt
 * @date 2021-06-29
 */
@Api(tags = "胎圈定额设定")
@Controller
@RequestMapping("/tq/quota")
public class TqQuotaSettingController extends BaseController {

    @Autowired
    private ITqQuotaSettingService iTqQuotaSettingService;

    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    private String prefix = "tq/quota";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tq:quota:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/quota";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tqQuotaSetting", new TqQuotaSetting());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tqQuotaSetting", iTqQuotaSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询胎圈定额设定列表
     */
    @ApiOperation("根据条件查询胎圈定额设定列表")
    @RequiresPermissions("tq:quota:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqQuotaSetting entity) {
        return iTqQuotaSettingService.list(entity);
    }

    /**
     * 修改或新增胎圈定额设定
     */
    @ApiOperation("修改或新增胎圈定额设定")
    @RequiresPermissions("tq:quota:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TqQuotaSetting tqQuotaSetting) {
        AjaxResult ajaxResult = null;
        String unique = iTqQuotaSettingService.checkTqQuotaSettingUnique(tqQuotaSetting);
        if (UserConstants.UNIQUE.equals(unique)) {
            if (tqQuotaSetting.getId() != null) {
                ajaxResult = iTqQuotaSettingService.edit(tqQuotaSetting);
            } else {
                ajaxResult = iTqQuotaSettingService.add(tqQuotaSetting);
            }
        } else {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.quota.uniqueError"));
        }
        return ajaxResult;
    }

    /**
     * 删除胎圈定额设定
     */
    @ApiOperation("删除胎圈定额设定（id不为空）")
    @RequiresPermissions("tq:quota:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqQuotaSettingService.remove(arr);
    }


    /**
     * 导出胎圈定额设定
     */
    @ApiOperation("导出胎圈定额设定")
    @RequiresPermissions("tq:quota:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TqQuotaSetting tqQuotaSetting) throws IOException {
        List<TqQuotaSetting> list = iTqQuotaSettingService.getList(tqQuotaSetting);
        ExcelUtil<TqQuotaSetting> util = new ExcelUtil(TqQuotaSetting.class);
        String fileName = I18nUtil.getMessage("ui.tq.quota.export.sheetName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, tqQuotaSetting.toString(), ApsConstant.PROCEDURE_CODE_TQ);
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
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.tq.quota.export.sheetName");
        ExcelUtil<TqQuotaSettingTemp> util = new ExcelUtil(TqQuotaSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("tq:quota:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TQ,
                I18nUtil.getMessage("ui.tq.quota.export.sheetName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TqQuotaSetting> util = new ExcelUtil<>(TqQuotaSetting.class);
        List<TqQuotaSetting> list = util.importExcel(in);
        AjaxResult ajaxResult = iTqQuotaSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
