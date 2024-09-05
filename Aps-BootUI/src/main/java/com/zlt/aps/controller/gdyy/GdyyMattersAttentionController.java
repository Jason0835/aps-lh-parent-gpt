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
import com.zlt.aps.gdyy.api.domain.dto.GdyyMattersAttentionDto;
import com.zlt.aps.gdyy.api.service.GdyyMattersAttentionService;
import com.zlt.aps.template.gdyy.GdyyMattersAttentionTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Api(tags = {"钢带大卷注意事项管理"})
@Controller
@RequestMapping("/gdyy/gdyyMattersAttention")
public class GdyyMattersAttentionController extends BaseController {

    private final String prefix = "gdyy/gdyyMattersAttention";

    @Resource
    private GdyyMattersAttentionService gdyyMattersAttentionService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @RequiresPermissions("gdyy:gdyyMattersAttention:view")
    @GetMapping()
    public String gdyyMattersAttention() {
        return prefix + "/gdyyMattersAttention";
    }

    @ApiOperation("根据条件查询帘布大卷注意事项列表")
    @RequiresPermissions("gdyy:gdyyMattersAttention:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyyMattersAttentionDto dto) {
        return gdyyMattersAttentionService.listGdyyMattersAttention(dto);
    }

    @ApiOperation("跳转到钢带大卷注意事项新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("gdyyMattersAttention", new GdyyMattersAttentionDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取钢带大卷注意事项信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("gdyyMattersAttention", gdyyMattersAttentionService.getGdyyMattersAttention(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改钢带大卷注意事项信息(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions({"gdyy:gdyyMattersAttention:edit", "gdyy:gdyyMattersAttention:add"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveMattersAttention(GdyyMattersAttentionDto dto) {
        return gdyyMattersAttentionService.saveGdyyMattersAttention(dto);
    }

    @ApiOperation("刪除钢带大卷注意事项信息")
    @RequiresPermissions("gdyy:gdyyMattersAttention:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return gdyyMattersAttentionService.deleteGdyyMattersAttention(arr);
    }

    @ApiOperation("导出钢带大卷注意事项信息")
    @RequiresPermissions("gdyy:gdyyMattersAttention:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GdyyMattersAttentionDto dto) throws IOException {
        List<GdyyMattersAttentionDto> list = gdyyMattersAttentionService.exportData(dto);
        ExcelUtil<GdyyMattersAttentionDto> util = new ExcelUtil<>(GdyyMattersAttentionDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.gdyy.params.mattersAttentionName");
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
        String fileName = I18nUtil.getMessage("ui.gdyy.gdyyMattersAttention.export.fileName");
        ExcelUtil<GdyyMattersAttentionTemp> util = new ExcelUtil<>(GdyyMattersAttentionTemp.class);
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
    @RequiresPermissions("gdyy:gdyyMattersAttention:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GDYY,
                I18nUtil.getMessage("ui.gdyy.gdyyMattersAttention.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<GdyyMattersAttentionDto> util = new ExcelUtil<>(GdyyMattersAttentionDto.class);
        List<GdyyMattersAttentionDto> list = util.importExcel(in);
        AjaxResult ajaxResult = gdyyMattersAttentionService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}