package com.zlt.aps.controller.gsq;

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
import com.zlt.aps.gsq.api.domain.dto.GsqSteelTypeColorDto;
import com.zlt.aps.gsq.api.service.IGsqSteelTypeColorService;
import com.zlt.aps.template.gsq.GsqSteelTypeColorTemp;
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

@Api(tags = {"钢带大卷颜色提示信息维护"})
@Controller
@RequestMapping("/gsq/gsqSteelTypeColor")
public class GsqSteelTypeColorController extends BaseController {

    private final String prefix = "gsq/gsqSteelTypeColor";

    @Resource
    private IGsqSteelTypeColorService gsqSteelTypeColorService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @RequiresPermissions("gsq:gsqSteelTypeColor:view")
    @GetMapping()
    public String gsqSteelTypeColor() {
        return prefix + "/gsqSteelTypeColor";
    }

    @ApiOperation("根据条件查询帘布大卷颜色提示信息维护列表")
    @RequiresPermissions("gsq:gsqSteelTypeColor:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqSteelTypeColorDto dto) {
        return gsqSteelTypeColorService.listGsqSteelTypeColor(dto);
    }

    @ApiOperation("跳转到钢带大卷颜色提示信息新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("gsqSteelTypeColor", new GsqSteelTypeColorDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取钢带大卷颜色提示信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("gsqSteelTypeColor", gsqSteelTypeColorService.getGsqSteelTypeColor(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改钢带大卷颜色提示信息(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions("gsq:gsqSteelTypeColor:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueGroupOrder(GsqSteelTypeColorDto dto) {
        return gsqSteelTypeColorService.saveGsqSteelTypeColor(dto);
    }

    @ApiOperation("根据code判断主键是否已经存在")
    @PostMapping("/checkRollCodeUnique")
    @ResponseBody
    public String checkRollCodeUnique(GsqSteelTypeColorDto dto) {
        return gsqSteelTypeColorService.checkGsqSteelTypeColor(dto);
    }

    @ApiOperation("刪除钢带大卷颜色提示信息")
    @RequiresPermissions("gsq:gsqSteelTypeColor:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return gsqSteelTypeColorService.deleteGsqSteelTypeColor(arr);
    }

    @RequiresPermissions("gsq:gsqSteelTypeColor:export")
    @ApiOperation("导出钢丝圈颜色提示信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GsqSteelTypeColorDto dto) throws IOException {
        List<GsqSteelTypeColorDto> list = gsqSteelTypeColorService.exportData(dto);
        ExcelUtil<GsqSteelTypeColorDto> util = new ExcelUtil<>(GsqSteelTypeColorDto.class);
        String fileName = I18nUtil.getMessage("ui.steelType.column.modalName");
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
        String fileName = I18nUtil.getMessage("ui.steelType.column.modalName");
        ExcelUtil<GsqSteelTypeColorTemp> util = new ExcelUtil<>(GsqSteelTypeColorTemp.class);
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
    @RequiresPermissions("gsq:gsqSteelTypeColor:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GDYY,
                I18nUtil.getMessage("ui.steelType.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<GsqSteelTypeColorDto> util = new ExcelUtil<>(GsqSteelTypeColorDto.class);
        List<GsqSteelTypeColorDto> list = util.importExcel(in);
        AjaxResult ajaxResult = gsqSteelTypeColorService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}