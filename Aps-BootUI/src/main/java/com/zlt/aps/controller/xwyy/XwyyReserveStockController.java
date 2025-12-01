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
import com.zlt.aps.xwyy.api.domain.dto.XwyyReserveStockDto;
import com.zlt.aps.xwyy.api.service.IXwyyReserveStockService;
import com.zlt.aps.template.xwyy.XwyyReserveStockTemp;
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
 * 纤维压延预生产库存倍数设定Controller
 *
 * @author hak
 * @date 2025-02-11
 */
@Api(tags = "纤维压延预生产库存倍数设定")
@Controller
@RequestMapping("/xwyy/reserveStock")
public class XwyyReserveStockController extends BaseController {

    private final String prefix = "xwyy/reserveStock";
    @Autowired
    private IXwyyReserveStockService iXwyyReserveStockService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @ApiOperation("跳转到纤维压延预生产库存倍数设定信息首页")
    @RequiresPermissions("xwyy:reserveStock:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/reserveStock";
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转到纤维压延预生产库存倍数设定信息新增页")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("reserveStockSetting", new XwyyReserveStockDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @ApiOperation("跳转到纤维压延预生产库存倍数设定信息编辑页")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("reserveStockSetting", iXwyyReserveStockService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询纤维压延预生产库存倍数设定列表
     */
    @ApiOperation("根据条件查询纤维压延预生产库存倍数设定列表")
    @RequiresPermissions("xwyy:reserveStock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyReserveStockDto dto) {
        return iXwyyReserveStockService.list(dto);
    }

    /**
     * 修改或新增纤维压延预生产库存倍数设定
     */
    @ApiOperation("修改或新增纤维压延预生产库存倍数设定")
    @RequiresPermissions({"xwyy:reserveStock:edit", "xwyy:reserveStock:add"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(XwyyReserveStockDto dto) {
        return iXwyyReserveStockService.edit(dto);
    }

    /**
     * 删除纤维压延预生产库存倍数设定
     */
    @ApiOperation("删除纤维压延预生产库存倍数设定（id不为空）")
    @RequiresPermissions("xwyy:reserveStock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iXwyyReserveStockService.remove(arr);
    }

    /**
     * 导出纤维压延预生产库存倍数设定
     */
    @ApiOperation("导出纤维压延预生产库存倍数设定")
    @RequiresPermissions("xwyy:reserveStock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, XwyyReserveStockDto dto) throws IOException {
        List<XwyyReserveStockDto> list = iXwyyReserveStockService.exportData(dto);
        ExcelUtil<XwyyReserveStockDto> util = new ExcelUtil<>(XwyyReserveStockDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.xwyy.reserveStock.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_XWYY);
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
        String fileName = I18nUtil.getMessage("ui.xwyy.reserveStock.export.fileName");
        ExcelUtil<XwyyReserveStockTemp> util = new ExcelUtil<>(XwyyReserveStockTemp.class);
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
    @RequiresPermissions("xwyy:reserveStock:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_XWYY,
                I18nUtil.getMessage("ui.xwyy.reserveStock.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<XwyyReserveStockDto> util = new ExcelUtil<>(XwyyReserveStockDto.class);
        List<XwyyReserveStockDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iXwyyReserveStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


}
