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
import com.zlt.aps.gdyy.api.domain.dto.GdyySteelRollColorDto;
import com.zlt.aps.gdyy.api.service.GdyySteelRollColorService;
import com.zlt.aps.template.gdyy.GdyySteelRollColorTemp;
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
@RequestMapping("/gdyy/gdyySteelRollColor")
public class GdyySteelRollColorController extends BaseController {

    private final String prefix = "gdyy/gdyySteelRollColor";

    @Resource
    private GdyySteelRollColorService gdyySteelRollColorService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @RequiresPermissions("gdyy:gdyySteelRollColor:view")
    @GetMapping()
    public String gdyySteelRollColor() {
        return prefix + "/gdyySteelRollColor";
    }

    @ApiOperation("根据条件查询帘布大卷颜色提示信息维护列表")
    @RequiresPermissions("gdyy:gdyySteelRollColor:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyySteelRollColorDto dto) {
        return gdyySteelRollColorService.listGdyySteelRollColor(dto);
    }

    @ApiOperation("跳转到钢带大卷颜色提示信息新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("gdyySteelRollColor", new GdyySteelRollColorDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取钢带大卷颜色提示信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("gdyySteelRollColor", gdyySteelRollColorService.getGdyySteelRollColor(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改钢带大卷颜色提示信息(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions("gdyy:gdyySteelRollColor:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueGroupOrder(GdyySteelRollColorDto dto) {
        return gdyySteelRollColorService.saveGdyySteelRollColor(dto);
    }

    @ApiOperation("根据code判断主键是否已经存在")
    @PostMapping("/checkRollCodeUnique")
    @ResponseBody
    public String checkRollCodeUnique(GdyySteelRollColorDto dto) {
        return gdyySteelRollColorService.checkGdyySteelRollColor(dto);
    }

    @ApiOperation("刪除钢带大卷颜色提示信息")
    @RequiresPermissions("gdyy:gdyySteelRollColor:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return gdyySteelRollColorService.deleteGdyySteelRollColor(arr);
    }

    @RequiresPermissions("gdyy:gdyySteelRollColor:export")
    @ApiOperation("导出钢带大卷颜色提示信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GdyySteelRollColorDto dto) throws IOException {
        List<GdyySteelRollColorDto> list = gdyySteelRollColorService.exportData(dto);
        ExcelUtil<GdyySteelRollColorDto> util = new ExcelUtil<>(GdyySteelRollColorDto.class);
        String fileName = I18nUtil.getMessage("ui.steelRollColor.column.modalName");
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
        String fileName = I18nUtil.getMessage("ui.steelRollColor.column.fileName");
        ExcelUtil<GdyySteelRollColorTemp> util = new ExcelUtil<>(GdyySteelRollColorTemp.class);
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
    @RequiresPermissions("gdyy:gdyySteelRollColor:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GDYY,
                I18nUtil.getMessage("ui.steelRollColor.column.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<GdyySteelRollColorDto> util = new ExcelUtil<>(GdyySteelRollColorDto.class);
        List<GdyySteelRollColorDto> list = util.importExcel(in);
        AjaxResult ajaxResult = gdyySteelRollColorService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}