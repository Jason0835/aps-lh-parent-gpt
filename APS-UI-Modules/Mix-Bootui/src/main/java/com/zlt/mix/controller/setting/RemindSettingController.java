package com.zlt.mix.controller.setting;

import com.zlt.mix.common.core.utils.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.mix.common.core.constant.ZltConstant;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import org.apache.poi.ss.usermodel.Workbook;
import com.zlt.mix.common.utils.ExportUtil;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.mix.common.utils.ImportUtil;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.io.*;

import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import com.zlt.mix.setting.api.domain.entity.RemindSetting;
import com.zlt.mix.setting.api.service.IRemindSettingService;

/**
 * 提醒设备Controller
 *
 * @author Gim
 * @date 2022-03-23
 */
@Api(tags = "提醒设备")
@Controller
@RequestMapping("/setting/remindSetting")
public class RemindSettingController extends BaseController {

    @Resource
    private IRemindSettingService iRemindSettingService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/remindSetting";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:remindSetting:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/remindSetting";
    }

    @ApiOperation("根据条件查询提醒设备列表")
    @RequiresPermissions("setting:remindSetting:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listRemindSetting(RemindSetting entity) {
        return iRemindSettingService.listRemindSetting(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("remindSetting", new RemindSetting());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("remindSetting", iRemindSettingService.getRemindSettingInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增提醒设备")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveRemindSetting(RemindSetting remindSetting) {
        return iRemindSettingService.saveRemindSetting(remindSetting);
    }

    @ApiOperation("删除提醒设备（id不为空）")
    @RequiresPermissions("setting:remindSetting:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeRemindSetting(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iRemindSettingService.deleteRemindSetting(arr);
    }

    @ApiOperation("校验提醒设备唯一性")
    @PostMapping("/checkRemindSettingUnique")
    @ResponseBody
    public String checkRemindSettingUnique(RemindSetting remindSetting) {
        return iRemindSettingService.checkRemindSettingUnique(remindSetting);
    }

    /**
     * 导出提醒设备
     */
    @ApiOperation("导出提醒设备")
    @RequiresPermissions("setting:remindSetting:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, RemindSetting remindSetting) throws IOException {
        String fileName = I18nUtil.getMessage("setting.remindSetting.modelName");
        List<RemindSetting> list = iRemindSettingService.exportData(remindSetting);
        ExcelUtil<RemindSetting> util = new ExcelUtil<>(RemindSetting.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, remindSetting.toString(), ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.remindSetting.modelName");
        ExcelUtil<RemindSetting> util = new ExcelUtil<>(RemindSetting.class);
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
    @RequiresPermissions("setting:remindSetting:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.remindSetting.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<RemindSetting> util = new ExcelUtil<>(RemindSetting.class);
        List<RemindSetting> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iRemindSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
