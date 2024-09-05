package com.zlt.aps.controller.cx;

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
import com.zlt.aps.cx.api.domain.dto.CxStockLocationSortDto;
import com.zlt.aps.cx.api.service.ICxStockLocationSortService;
import com.zlt.aps.template.cx.CxStockLocationSortTemp;
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
 * 库存地点生产顺序Controller
 *
 * @author chen
 * @date 2021-07-22
 */
@Api(tags = "库存地点生产顺序")
@Controller
@RequestMapping("/cx/stockLocationSort")
public class CxStockLocationSortController extends BaseController {

    private final String prefix = "cx/stockLocationSort";

    @Autowired
    private ICxStockLocationSortService iCxStockLocationSortService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:stockLocationSort:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/stockLocationSort";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxStockLocationSort", new CxStockLocationSortDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxStockLocationSort", iCxStockLocationSortService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询库存地点生产顺序列表
     */
    @ApiOperation("根据条件查询库存地点生产顺序列表")
    @RequiresPermissions("cx:stockLocationSort:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxStockLocationSortDto dto) {
        return iCxStockLocationSortService.list(dto);
    }

    /**
     * 修改或新增库存地点生产顺序
     */
    @ApiOperation("修改或新增库存地点生产顺序")
    @RequiresPermissions("cx:stockLocationSort:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxStockLocationSortDto dto) {
        AjaxResult ajaxResult = null;
        if (dto.getId() != null) {
            ajaxResult = iCxStockLocationSortService.edit(dto);
        } else {
            ajaxResult = iCxStockLocationSortService.add(dto);
        }
        return ajaxResult;
    }

    /**
     * 删除库存地点生产顺序
     */
    @ApiOperation("删除库存地点生产顺序（id不为空）")
    @RequiresPermissions("cx:stockLocationSort:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxStockLocationSortService.remove(arr);
    }

    @ApiOperation("校验库存地点生产顺序唯一性")
    @PostMapping("/checkCxStockLocationSortUnique")
    @ResponseBody
    public String checkCxStockLocationSortUnique(CxStockLocationSortDto dto) {
        return iCxStockLocationSortService.checkCxStockLocationSortUnique(dto);
    }

    /**
     * 导出库存地点生产顺序
     */
    @ApiOperation("导出库存地点生产顺序")
    @RequiresPermissions("cx:stockLocationSort:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxStockLocationSortDto dto) throws IOException {
        List<CxStockLocationSortDto> list = iCxStockLocationSortService.getList(dto);
        ExcelUtil<CxStockLocationSortDto> util = new ExcelUtil<>(CxStockLocationSortDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.stockLocationSort.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }


    /**
     * 下载模板
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.stockLocationSort.modalName");
        ExcelUtil<CxStockLocationSortTemp> util = new ExcelUtil<>(CxStockLocationSortTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:stockLocationSort:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.stockLocationSort.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxStockLocationSortDto> util = new ExcelUtil<>(CxStockLocationSortDto.class);
        List<CxStockLocationSortDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxStockLocationSortService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
