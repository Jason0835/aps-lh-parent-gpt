package com.zlt.aps.controller.nc;

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
import com.zlt.aps.nc.api.domain.entity.NcQuotaSetting;
import com.zlt.aps.nc.api.service.INcQuotaSettingService;
import com.zlt.aps.template.nc.NcQuotaSettingTemp;
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
 * 内衬定额设定Controller
 *
 * @author zlt
 * @date 2021-06-29
 */
@Api(tags = "内衬定额设定")
@Controller
@RequestMapping("/nc/quota")
public class NcQuotaSettingController extends BaseController {

    private String prefix = "nc/quota";

    @Autowired
    private INcQuotaSettingService iNcQuotaSettingService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    /**
     * 跳转至主页面
     */
    @RequiresPermissions("nc:quota:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/quota";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("ncQuotaSetting", new NcQuotaSetting());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("ncQuotaSetting", iNcQuotaSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询内衬定额设定列表
     */
    @ApiOperation("根据条件查询内衬定额设定列表")
    @RequiresPermissions("nc:quota:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcQuotaSetting entity) {
        return iNcQuotaSettingService.list(entity);
    }

    /**
     * 修改或新增内衬定额设定
     */
    @ApiOperation("修改或新增内衬定额设定")
    @RequiresPermissions("nc:quota:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(NcQuotaSetting ncQuotaSetting) {
        AjaxResult ajaxResult = null;
        String unique = iNcQuotaSettingService.checkNcQuotaSettingUnique(ncQuotaSetting);
        if (UserConstants.UNIQUE.equals(unique)) {
            if (ncQuotaSetting.getId() != null) {
                ajaxResult = iNcQuotaSettingService.edit(ncQuotaSetting);
            } else {
                ajaxResult = iNcQuotaSettingService.add(ncQuotaSetting);
            }
        } else {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.quota.uniqueError"));
        }
        return ajaxResult;
    }

    /**
     * 删除内衬定额设定
     */
    @ApiOperation("删除内衬定额设定（id不为空）")
    @RequiresPermissions("nc:quota:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcQuotaSettingService.remove(arr);
    }


    /**
     * 导出内衬定额设定
     */
    @ApiOperation("导出内衬定额设定")
    @RequiresPermissions("nc:quota:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, NcQuotaSetting ncQuotaSetting) throws IOException {
        List<NcQuotaSetting> list = iNcQuotaSettingService.getList(ncQuotaSetting);
        ExcelUtil<NcQuotaSetting> util = new ExcelUtil(NcQuotaSetting.class);
        String fileName = I18nUtil.getMessage("ui.nc.quota.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, ncQuotaSetting.toString(), ApsConstant.PROCEDURE_CODE_NC);
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
        String fileName = I18nUtil.getMessage("ui.nc.quota.export.fileName");
        ExcelUtil<NcQuotaSettingTemp> util = new ExcelUtil<>(NcQuotaSettingTemp.class);
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
    @RequiresPermissions("nc:quota:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_NC,
                I18nUtil.getMessage("ui.nc.quota.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //解析文件
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<NcQuotaSetting> util = new ExcelUtil<>(NcQuotaSetting.class);
        List<NcQuotaSetting> list = util.importExcel(in);
        AjaxResult ajaxResult = iNcQuotaSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
