package com.zlt.aps.controller.cd15;

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
import com.zlt.aps.cd15.api.domain.dto.Cd15LossSettingDto;
import com.zlt.aps.cd15.api.service.ICd15LossSettingService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd15.Cd15LossSettingTemp;
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
 * 15度裁断损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@Api(tags = "15度裁断损耗率设定")
@Controller
@RequestMapping("/cd15/loss")
public class Cd15LossSettingController extends BaseController {

    private final String prefix = "cd15/loss";
    @Autowired
    private ICd15LossSettingService iCd15LossSettingService;
    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cd15:loss:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/loss";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cd15LossSetting", new Cd15LossSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cd15LossSetting", iCd15LossSettingService.getInfo(id));
        return prefix + "/edit";
    }


    @ApiOperation("刪除全部")
    @RequiresPermissions("cd15:loss:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iCd15LossSettingService.deleteAll();
    }

    /**
     * 根据条件查询15度裁断损耗率设定列表
     */
    @ApiOperation("根据条件查询15度裁断损耗率设定列表")
    @RequiresPermissions("cd15:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15LossSettingDto dto) {
        return iCd15LossSettingService.list(dto);
    }

    /**
     * 修改或新增15度裁断损耗率设定
     */
    @ApiOperation("修改或新增15度裁断损耗率设定")
    @RequiresPermissions("cd15:loss:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Cd15LossSettingDto dto) {
        AjaxResult ajaxResult = null;
        if (dto.getId() != null) {
            ajaxResult = iCd15LossSettingService.edit(dto);
        } else {
            ajaxResult = iCd15LossSettingService.add(dto);
        }
        return ajaxResult;
    }

    /**
     * 删除15度裁断损耗率设定
     */
    @ApiOperation("删除15度裁断损耗率设定（id不为空）")
    @RequiresPermissions("cd15:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCd15LossSettingService.remove(arr);
    }


    @ApiOperation("校验15度裁断损耗率设定唯一性")
    @PostMapping("/checkCd15LossSettingUnique")
    @ResponseBody
    public String checkCd15LossSettingUnique(Cd15LossSettingDto dto) {
        return iCd15LossSettingService.checkCd15LossSettingUnique(dto);
    }


    /**
     * 导出15度裁断损耗率设定
     */
    @ApiOperation("导出15度裁断损耗率设定")
    @RequiresPermissions("cd15:loss:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd15LossSettingDto dto) throws IOException {
        List<Cd15LossSettingDto> list = iCd15LossSettingService.getList(dto);
        ExcelUtil<Cd15LossSettingDto> util = new ExcelUtil<>(Cd15LossSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cd15.loss.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CD15);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.cd15.loss.modelName");
        ExcelUtil<Cd15LossSettingTemp> util = new ExcelUtil<>(Cd15LossSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("cd15:loss:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<Cd15LossSettingDto> util = new ExcelUtil<>(Cd15LossSettingDto.class);
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD15,
                I18nUtil.getMessage("ui.data.column.cd15.loss.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        List<Cd15LossSettingDto> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = iCd15LossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
