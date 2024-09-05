package com.zlt.aps.controller.tm;

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
import com.zlt.aps.template.tm.TmLossSettingTemp;
import com.zlt.aps.tm.api.domain.dto.TmLossSettingDto;
import com.zlt.aps.tm.api.service.ITmLossSettingService;
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
 * 胎面损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-12
 */
@Api(tags = "胎面损耗率设定")
@Controller
@RequestMapping("/tm/loss")
public class TmLossSettingController extends BaseController {

    private final String prefix = "tm/loss";
    @Autowired
    private ITmLossSettingService iTmLossSettingService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tm:loss:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/loss";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tmLossSetting", new TmLossSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tmLossSetting", iTmLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询胎面损耗率设定列表
     */
    @ApiOperation("根据条件查询胎面损耗率设定列表")
    @RequiresPermissions("tm:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmLossSettingDto entity) {
        return iTmLossSettingService.list(entity);
    }

    /**
     * 修改或新增胎面损耗率设定
     */
    @ApiOperation("修改或新增胎面损耗率设定")
    @RequiresPermissions("tm:loss:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TmLossSettingDto tmLossSetting) {
        AjaxResult ajaxResult = null;
        if (tmLossSetting.getId() != null) {
            ajaxResult = iTmLossSettingService.edit(tmLossSetting);
        } else {
            ajaxResult = iTmLossSettingService.add(tmLossSetting);
        }
        return ajaxResult;
    }

    /**
     * 删除胎面损耗率设定
     */
    @ApiOperation("删除胎面损耗率设定（id不为空）")
    @RequiresPermissions("tm:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTmLossSettingService.remove(arr);
    }


    @ApiOperation("刪除全部")
    @RequiresPermissions("tm:loss:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTmLossSettingService.deleteAll();
    }


    @ApiOperation("校验胎面损耗率设定唯一性")
    @PostMapping("/checkTmLossSettingUnique")
    @ResponseBody
    public String checkTmLossSettingUnique(TmLossSettingDto tmLossSetting) {
        return iTmLossSettingService.checkTmLossSettingUnique(tmLossSetting);
    }

    /**
     * 导出胎面损耗率设定
     */
    @ApiOperation("导出胎面损耗率设定")
    @RequiresPermissions("tm:loss:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TmLossSettingDto tmLossSetting) throws IOException {
        List<TmLossSettingDto> list = iTmLossSettingService.getList(tmLossSetting);
        ExcelUtil<TmLossSettingDto> util = new ExcelUtil<>(TmLossSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.tm.loss.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, tmLossSetting.toString(), ApsConstant.PROCEDURE_CODE_TM);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tm.loss.modelName");
        ExcelUtil<TmLossSettingTemp> util = new ExcelUtil<>(TmLossSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("tm:loss:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TM,
                I18nUtil.getMessage("ui.data.column.tm.loss.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TmLossSettingDto> util = new ExcelUtil<>(TmLossSettingDto.class);
        List<TmLossSettingDto> list = util.importExcel(in);

        // 导入
        AjaxResult ajaxResult = iTmLossSettingService.importData(list, updateSupport, importLog.getId());

        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);

        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
