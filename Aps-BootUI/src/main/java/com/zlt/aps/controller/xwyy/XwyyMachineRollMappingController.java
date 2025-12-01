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
import com.zlt.aps.template.xwyy.XwyyMachineRollMappingTemp;
import com.zlt.aps.xwyy.api.domain.dto.XwyyMachineRollMappingDto;
import com.zlt.aps.xwyy.api.service.XwyyMachineRollMappingService;
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

/**
 * <p>
 * 纤维压延帘布大卷与机台的映射表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@Api(tags = {"纤维压延帘布大卷与机台的映射表"})
@Controller
@RequestMapping("/xwyy/xwyyMachineRollMapping")
public class XwyyMachineRollMappingController extends BaseController {
    private String prefix = "xwyy/xwyyMachineRollMapping";

    @Resource
    private XwyyMachineRollMappingService xwyyMachineRollMappingService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @RequiresPermissions("xwyy:xwyyMachineRollMapping:view")
    @GetMapping()
    public String xwyyMachineRollMapping() {
        return prefix + "/xwyyMachineRollMapping";
    }

    @ApiOperation("根据条件查询纤维压延帘布大卷与机台的映射表列表")
    @RequiresPermissions("xwyy:xwyyMachineRollMapping:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyMachineRollMappingDto dto) {
        return xwyyMachineRollMappingService.listXwyyMachineRollMapping(dto);
    }

    @ApiOperation("跳转到纤维压延帘布大卷与机台的映射表新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("xwyyMachineRollMapping", new XwyyMachineRollMappingDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取纤维压延帘布大卷与机台的映射表，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("xwyyMachineRollMapping", xwyyMachineRollMappingService.getXwyyBigRollColor(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改纤维压延帘布大卷与机台的映射表(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions({"xwyy:xwyyMachineRollMapping:edit", "xwyy:xwyyMachineRollMapping:add"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueGroupOrder(XwyyMachineRollMappingDto dto) {
        return xwyyMachineRollMappingService.saveXwyyMachineRollMapping(dto);
    }

    @ApiOperation("根据id判断主键是否已经存在")
    @PostMapping("/checkRollCodeUnique")
    @ResponseBody
    public String checkRollCodeUnique(XwyyMachineRollMappingDto dto) {
        return xwyyMachineRollMappingService.checkXwyyMachineRollMapping(dto);
    }

    @ApiOperation("刪除纤维压延帘布大卷与机台的映射表")
    @RequiresPermissions("xwyy:xwyyMachineRollMapping:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return xwyyMachineRollMappingService.deleteXwyyMachineRollMapping(arr);
    }

    @ApiOperation("导出纤维压延帘布大卷与机台的映射表")
    @RequiresPermissions("xwyy:xwyyMachineRollMapping:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, XwyyMachineRollMappingDto dto) throws IOException {
        List<XwyyMachineRollMappingDto> list = xwyyMachineRollMappingService.exportData(dto);
        ExcelUtil<XwyyMachineRollMappingDto> util = new ExcelUtil<>(XwyyMachineRollMappingDto.class);
        String fileName = I18nUtil.getMessage("ui.MachineRollMapping.column.modalName");
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
        String fileName = I18nUtil.getMessage("ui.MachineRollMapping.column.modalName");
        ExcelUtil<XwyyMachineRollMappingTemp> util = new ExcelUtil<>(XwyyMachineRollMappingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("xwyy:xwyyMachineRollMapping:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data,ApsConstant.PROCEDURE_CODE_XWYY, I18nUtil.getMessage("ui.MachineRollMapping.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<XwyyMachineRollMappingDto> util = new ExcelUtil<>(XwyyMachineRollMappingDto.class);
        List<XwyyMachineRollMappingDto> list = util.importExcel(in);
        AjaxResult ajaxResult = xwyyMachineRollMappingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
