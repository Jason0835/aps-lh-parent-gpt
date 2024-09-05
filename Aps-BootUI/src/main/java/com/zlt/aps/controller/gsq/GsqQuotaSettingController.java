package com.zlt.aps.controller.gsq;

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
import com.zlt.aps.gsq.api.domain.entity.GsqQuotaSetting;
import com.zlt.aps.gsq.api.service.IGsqQuotaSettingService;
import com.zlt.aps.template.gsq.GsqQuotaSettingTemp;
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
 * 钢丝圈定额设定Controller
 *
 * @author zlt
 * @date 2021-06-29
 */
@Api(tags = "钢丝圈定额设定")
@Controller
@RequestMapping("/gsq/quota")
public class GsqQuotaSettingController extends BaseController {

    @Autowired
    private IGsqQuotaSettingService iGsqQuotaSettingService;

    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    private String prefix = "gsq/quota";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("gsq:quota:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/quota";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("gsqQuotaSetting", new GsqQuotaSetting());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("gsqQuotaSetting", iGsqQuotaSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询钢丝圈定额设定列表
     */
    @ApiOperation("根据条件查询钢丝圈定额设定列表")
    @RequiresPermissions("gsq:quota:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqQuotaSetting entity) {
        return iGsqQuotaSettingService.list(entity);
    }

    /**
     * 修改或新增钢丝圈定额设定
     */
    @ApiOperation("修改或新增钢丝圈定额设定")
    @RequiresPermissions("gsq:quota:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GsqQuotaSetting gsqQuotaSetting) {
        AjaxResult ajaxResult = null;
        String unique = iGsqQuotaSettingService.checkGsqQuotaSettingUnique(gsqQuotaSetting);
        if (UserConstants.UNIQUE.equals(unique)) {
            if (gsqQuotaSetting.getId() != null) {
                ajaxResult = iGsqQuotaSettingService.edit(gsqQuotaSetting);
            } else {
                ajaxResult = iGsqQuotaSettingService.add(gsqQuotaSetting);
            }
        } else {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.quota.uniqueError"));
        }
        return ajaxResult;
    }

    /**
     * 删除钢丝圈定额设定
     */
    @ApiOperation("删除钢丝圈定额设定（id不为空）")
    @RequiresPermissions("gsq:quota:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGsqQuotaSettingService.remove(arr);
    }


    /**
     * 导出钢丝圈定额设定
     */
    @ApiOperation("导出钢丝圈定额设定")
    @RequiresPermissions("gsq:quota:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GsqQuotaSetting gsqQuotaSetting) throws IOException {
        List<GsqQuotaSetting> list = iGsqQuotaSettingService.getList(gsqQuotaSetting);
        ExcelUtil<GsqQuotaSetting> util = new ExcelUtil<>(GsqQuotaSetting.class);
        String fileName = I18nUtil.getMessage("ui.gsq.quota.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, gsqQuotaSetting.toString(), ApsConstant.PROCEDURE_CODE_GSQ);
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
        String fileName = I18nUtil.getMessage("ui.gsq.quota.export.fileName");
        ExcelUtil<GsqQuotaSettingTemp> util = new ExcelUtil<>(GsqQuotaSettingTemp.class);
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
    @RequiresPermissions("gsq:quota:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GSQ,
                I18nUtil.getMessage("ui.gsq.quota.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<GsqQuotaSetting> util = new ExcelUtil<>(GsqQuotaSetting.class);
        List<GsqQuotaSetting> list = util.importExcel(in);
        AjaxResult ajaxResult = iGsqQuotaSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
