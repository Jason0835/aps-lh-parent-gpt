package com.zlt.aps.controller.nc;

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
import com.zlt.aps.nc.api.domain.dto.NcLossSettingDto;
import com.zlt.aps.nc.api.service.INcLossSettingService;
import com.zlt.aps.template.nc.NcLossSettingTemp;
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
 * 内衬损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-13
 */
@Api(tags = "内衬损耗率设定")
@Controller
@RequestMapping("/nc/loss")
public class NcLossSettingController extends BaseController {

    private final String prefix = "nc/loss";
    @Autowired
    private INcLossSettingService iNcLossSettingService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("nc:loss:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/loss";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("ncLossSetting", new NcLossSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("ncLossSetting", iNcLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询内衬损耗率设定列表
     */
    @ApiOperation("根据条件查询内衬损耗率设定列表")
    @RequiresPermissions("nc:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcLossSettingDto dto) {
        return iNcLossSettingService.list(dto);
    }

    /**
     * 修改或新增内衬损耗率设定
     */
    @ApiOperation("修改或新增内衬损耗率设定")
    @RequiresPermissions("nc:loss:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(NcLossSettingDto dto) {
        AjaxResult ajaxResult = null;
        if (dto.getId() != null) {
            ajaxResult = iNcLossSettingService.edit(dto);
        } else {
            ajaxResult = iNcLossSettingService.add(dto);
        }
        return ajaxResult;
    }

    /**
     * 删除内衬损耗率设定
     */
    @ApiOperation("删除内衬损耗率设定（id不为空）")
    @RequiresPermissions("nc:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcLossSettingService.remove(arr);
    }


    @ApiOperation("刪除全部")
    @RequiresPermissions("nc:loss:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iNcLossSettingService.deleteAll();
    }


    @ApiOperation("校验内衬损耗率设定唯一性")
    @PostMapping("/checkNcLossSettingUnique")
    @ResponseBody
    public String checkNcLossSettingUnique(NcLossSettingDto dto) {
        return iNcLossSettingService.checkNcLossSettingUnique(dto);
    }


    /**
     * 导出内衬损耗率设定
     */
    @ApiOperation("导出内衬损耗率设定")
    @RequiresPermissions("nc:loss:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, NcLossSettingDto ncLossSetting) throws IOException {
        List<NcLossSettingDto> list = iNcLossSettingService.getList(ncLossSetting);
        ExcelUtil<NcLossSettingDto> util = new ExcelUtil<>(NcLossSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.nc.loss.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, ncLossSetting.toString(), ApsConstant.PROCEDURE_CODE_NC);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.nc.loss.modelName");
        ExcelUtil<NcLossSettingTemp> util = new ExcelUtil<>(NcLossSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("nc:loss:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_NC,
                I18nUtil.getMessage("ui.data.column.nc.loss.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<NcLossSettingDto> util = new ExcelUtil<>(NcLossSettingDto.class);
        List<NcLossSettingDto> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = iNcLossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
