package com.zlt.aps.controller.xwyy;

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
import com.zlt.aps.template.xwyy.XwyyBigRollColorTemp;
import com.zlt.aps.xwyy.api.domain.dto.XwyyBigRollColorDto;
import com.zlt.aps.xwyy.api.service.XwyyBigRollColorService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Api(tags = {"帘布大卷颜色提示信息维护"})
@Controller
@RequestMapping("/xwyy/xwyyBigRollColor")
public class xwyyBigRollColorController extends BaseController {

    private final String prefix = "xwyy/xwyyBigRollColor";

    @Resource
    private XwyyBigRollColorService xwyyBigRollColorService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;



    @RequiresPermissions("xwyy:xwyyBigRollColor:view")
    @GetMapping()
    public String xwyyBigRollColor() {
        return prefix + "/xwyyBigRollColor";
    }

    @ApiOperation("根据条件查询帘布大卷颜色提示信息维护列表")
    @RequiresPermissions("xwyy:xwyyBigRollColor:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyBigRollColorDto dto) {
        return xwyyBigRollColorService.listXwyyBigRollColor(dto);
    }

    @ApiOperation("跳转到帘布大卷颜色提示信息新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("xwyyBigRollColor", new XwyyBigRollColorDto());
        return prefix + "/edit";
    }


    @ApiOperation("获取帘布大卷颜色提示信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("xwyyBigRollColor", xwyyBigRollColorService.getXwyyBigRollColor(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改帘布大卷颜色提示信息(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions({"xwyy:xwyyBigRollColor:edit", "xwyy:xwyyBigRollColor:add"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueGroupOrder(XwyyBigRollColorDto dto) {
        return xwyyBigRollColorService.saveXwyyBigRollColor(dto);
    }

    @ApiOperation("根据id判断主键是否已经存在")
    @PostMapping("/checkRollCodeUnique")
    @ResponseBody
    public String checkRollCodeUnique(XwyyBigRollColorDto dto) {
        return xwyyBigRollColorService.checkXwyyBigRollColor(dto);
    }

    @ApiOperation("刪除帘布大卷颜色提示信息")
    @RequiresPermissions("xwyy:xwyyBigRollColor:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return xwyyBigRollColorService.deleteXwyyBigRollColor(arr);
    }

    @RequiresPermissions("xwyy:xwyyBigRollColor:export")
    @ApiOperation("导出帘布大卷颜色提示信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, XwyyBigRollColorDto dto) throws IOException {
        List<XwyyBigRollColorDto> list = xwyyBigRollColorService.exportData(dto);
        ExcelUtil<XwyyBigRollColorDto> util = new ExcelUtil<>(XwyyBigRollColorDto.class);
        String fileName = I18nUtil.getMessage("ui.bigRollColor.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_XWYY);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     */
    @ApiOperation("下载模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.bigRollColor.column.modalName");
        ExcelUtil<XwyyBigRollColorTemp> util = new ExcelUtil<>(XwyyBigRollColorTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("xwyy:xwyyBigRollColor:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_XWYY, I18nUtil.getMessage("ui.bigRollColor.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<XwyyBigRollColorDto> util = new ExcelUtil<>(XwyyBigRollColorDto.class);
        List<XwyyBigRollColorDto> list = util.importExcel(in);
        AjaxResult ajaxResult = xwyyBigRollColorService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}