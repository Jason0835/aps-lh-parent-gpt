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
import com.zlt.aps.cx.api.domain.dto.CxMonthStockDto;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cx.api.service.ICxMonthStockService;
import com.zlt.aps.cx.api.service.ICxProductConstructionInfoService;
import com.zlt.aps.template.cx.CxMonthStockTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 成型月结库存Controller
 *
 * @author chen
 * @date 2021-06-17
 */
@Controller
@RequestMapping("/cx/monthStock")
@Api(tags = {"成型月结库存信息接口"})
public class CxMonthStockController extends BaseController {
    private final String prefix = "cx/monthStock";

    @Autowired
    private ICxMonthStockService iCxMonthStockService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private ICxProductConstructionInfoService iCxProductConstructionInfoService;


    @RequiresPermissions("cx:monthStock:view")
    @GetMapping()
    @ApiOperation("跳转到成型月结库存信息首页")
    public String toIndex() {
        return prefix + "/monthStock";
    }

    /**
     * 查询成型月结库存信息维护列表
     */
    @RequiresPermissions("cx:monthStock:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询成型月结库存信息维护列表")
    public TableDataInfo list(CxMonthStockDto dto) {
        TableDataInfo list = iCxMonthStockService.list(dto);
        return list;
    }

    /**
     * 根据id获取成型月结库存信息维护详细信息
     */
    @RequiresPermissions("cx:monthStock:edit")
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取成型月结库存信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("monthStock", iCxMonthStockService.getInfo(id));
        List<CxProductConstructionInfo> pcList=new ArrayList<CxProductConstructionInfo>();
        mmap.put("embryoVersions", pcList);
        return prefix + "/edit";
    }

    @RequiresPermissions("cx:monthStock:add")
    @ApiOperation("跳转到成型月结库存新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("monthStock", new CxMonthStockDto());
        List<CxProductConstructionInfo> pcList=new ArrayList<CxProductConstructionInfo>();
        mmap.put("embryoVersions", pcList);
        return prefix + "/edit";
    }


    /**
     * 获取胎胚版本列表
     */
    @ApiOperation("获取胎胚版本列表")
    @PostMapping("/getProductEmbryoVersions")
    @ResponseBody
    public AjaxResult getEmbryoVersions(CxProductConstructionInfo cxProductConstructionInfo) {
        List<CxProductConstructionInfo> pcList = iCxProductConstructionInfoService.getList(cxProductConstructionInfo);
        return AjaxResult.success(pcList);
    }

    /**
     * 保存成型月结库存信息维护
     */
    @RequiresPermissions("cx:monthStock:edit")
    @PostMapping("/edit")
    @ResponseBody
    @ApiOperation("保存成型月结库存信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(CxMonthStockDto dto) {
        return iCxMonthStockService.edit(dto);
    }

    /**
     * 删除成型月结库存信息维护
     */
    @RequiresPermissions("cx:monthStock:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除成型月结库存信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxMonthStockService.remove(arr);
    }

    /**
     * 导出成型月结库存信息
     */
    @RequiresPermissions("cx:monthStock:export")
    @ApiOperation("导出成型月结库存信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxMonthStockDto dto) throws IOException {
        List<CxMonthStockDto> list = iCxMonthStockService.exportData(dto);
        ExcelUtil<CxMonthStockDto> util = new ExcelUtil<>(CxMonthStockDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cx.monthStock.modelName");
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
        String fileName = I18nUtil.getMessage("ui.data.column.cx.monthStock.modelName");
        ExcelUtil<CxMonthStockTemp> util = new ExcelUtil<>(CxMonthStockTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:monthStock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.cx.monthStock.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxMonthStockDto> util = new ExcelUtil<>(CxMonthStockDto.class);
        List<CxMonthStockDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxMonthStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
