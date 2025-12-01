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
import com.zlt.aps.gsq.api.domain.dto.GsqReserveStockDto;
import com.zlt.aps.gsq.api.service.IGsqReserveStockService;
import com.zlt.aps.template.gsq.GsqReserveStockTemp;
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
 * 钢丝圈预生产库存倍数设定Controller
 *
 * @author hak
 * @date 2025-02-11
 */
@Api(tags = "钢丝圈预生产库存倍数设定")
@Controller
@RequestMapping("/gsq/reserveStock")
public class GsqReserveStockController extends BaseController {

    private final String prefix = "gsq/reserveStock";
    @Autowired
    private IGsqReserveStockService iGsqReserveStockService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @ApiOperation("跳转到钢丝圈预生产库存倍数设定信息首页")
    @RequiresPermissions("gsq:reserveStock:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/reserveStock";
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转到钢丝圈预生产库存倍数设定信息新增页")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("reserveStockSetting", new GsqReserveStockDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @ApiOperation("跳转到钢丝圈预生产库存倍数设定信息编辑页")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("reserveStockSetting", iGsqReserveStockService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询钢丝圈预生产库存倍数设定列表
     */
    @ApiOperation("根据条件查询钢丝圈预生产库存倍数设定列表")
    @RequiresPermissions("gsq:reserveStock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqReserveStockDto dto) {
        return iGsqReserveStockService.list(dto);
    }

    /**
     * 修改或新增钢丝圈预生产库存倍数设定
     */
    @ApiOperation("修改或新增钢丝圈预生产库存倍数设定")
    @RequiresPermissions({"gsq:reserveStock:edit", "gsq:reserveStock:add"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GsqReserveStockDto dto) {
        return iGsqReserveStockService.edit(dto);
    }

    /**
     * 删除钢丝圈预生产库存倍数设定
     */
    @ApiOperation("删除钢丝圈预生产库存倍数设定（id不为空）")
    @RequiresPermissions("gsq:reserveStock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGsqReserveStockService.remove(arr);
    }

    /**
     * 导出钢丝圈预生产库存倍数设定
     */
    @ApiOperation("导出钢丝圈预生产库存倍数设定")
    @RequiresPermissions("gsq:reserveStock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GsqReserveStockDto dto) throws IOException {
        List<GsqReserveStockDto> list = iGsqReserveStockService.exportData(dto);
        ExcelUtil<GsqReserveStockDto> util = new ExcelUtil<>(GsqReserveStockDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.gsq.reserveStock.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_GSQ);
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
        String fileName = I18nUtil.getMessage("ui.data.column.gsq.reserveStock.modelName");
        ExcelUtil<GsqReserveStockTemp> util = new ExcelUtil<>(GsqReserveStockTemp.class);
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
    @RequiresPermissions("gsq:reserveStock:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GSQ,
                I18nUtil.getMessage("ui.data.column.gsq.reserveStock.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<GsqReserveStockDto> util = new ExcelUtil<>(GsqReserveStockDto.class);
        List<GsqReserveStockDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iGsqReserveStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


}
