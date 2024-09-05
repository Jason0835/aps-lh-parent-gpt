package com.zlt.aps.controller.gdyy;

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
import com.zlt.aps.gdyy.api.domain.dto.GdyyQuotaSettingDto;
import com.zlt.aps.gdyy.api.service.IGdyyQuotaSettingService;
import com.zlt.aps.template.gdyy.GdyyQuotaSettingTemp;
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
 * 钢带压延定额设定Controller
 *
 * @author chen
 * @date 2021-06-30
 */
@Api(tags = "钢带压延定额设定")
@Controller
@RequestMapping("/gdyy/quota")
public class GdyyQuotaSettingController extends BaseController {

    private final String prefix = "gdyy/quota";
    @Autowired
    private IGdyyQuotaSettingService iGdyyQuotaSettingService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @ApiOperation("跳转到钢带压延定额设定信息首页")
    @RequiresPermissions("gdyy:quota:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/quota";
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转到钢带压延定额设定信息新增页")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("quotaSetting", new GdyyQuotaSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @ApiOperation("跳转到钢带压延定额设定信息编辑页")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("quotaSetting", iGdyyQuotaSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询钢带压延定额设定列表
     */
    @ApiOperation("根据条件查询钢带压延定额设定列表")
    @RequiresPermissions("gdyy:quota:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyyQuotaSettingDto dto) {
        return iGdyyQuotaSettingService.list(dto);
    }

    /**
     * 修改或新增钢带压延定额设定
     */
    @ApiOperation("修改或新增钢带压延定额设定")
    @RequiresPermissions({"gdyy:quota:edit", "gdyy:quota:add"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GdyyQuotaSettingDto dto) {
        return iGdyyQuotaSettingService.edit(dto);
    }

    /**
     * 删除钢带压延定额设定
     */
    @ApiOperation("删除钢带压延定额设定（id不为空）")
    @RequiresPermissions("gdyy:quota:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGdyyQuotaSettingService.remove(arr);
    }

    /**
     * 导出钢带压延定额设定
     */
    @ApiOperation("导出钢带压延定额设定")
    @RequiresPermissions("gdyy:quota:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GdyyQuotaSettingDto dto) throws IOException {
        List<GdyyQuotaSettingDto> list = iGdyyQuotaSettingService.exportData(dto);
        ExcelUtil<GdyyQuotaSettingDto> util = new ExcelUtil<>(GdyyQuotaSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.gdyy.quota.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_GDYY);
        iExportLogService.add(exportLog);
    }


    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @ApiOperation("下载模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.gdyy.quota.export.fileName");
        ExcelUtil<GdyyQuotaSettingTemp> util = new ExcelUtil<>(GdyyQuotaSettingTemp.class);
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
    @RequiresPermissions("gdyy:quota:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GDYY,
                I18nUtil.getMessage("ui.gdyy.quota.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<GdyyQuotaSettingDto> util = new ExcelUtil<>(GdyyQuotaSettingDto.class);
        List<GdyyQuotaSettingDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iGdyyQuotaSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


}
