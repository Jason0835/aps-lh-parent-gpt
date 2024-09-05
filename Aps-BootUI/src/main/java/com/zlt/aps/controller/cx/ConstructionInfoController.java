package com.zlt.aps.controller.cx;


import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.dto.ConstructionInfoDto;
import com.zlt.aps.cx.api.service.ICxConstructionInfoService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

@Api(tags = {"施工信息接口"})
@Controller
@RequestMapping("/cx/constructionInfo")
public class ConstructionInfoController extends BaseController {

    private String prefix = "cx/constructionInfo";

    @Resource
    private ICxConstructionInfoService iCxConstructionInfoService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;


    @RequiresPermissions("cx:constructionInfo:view")
    @GetMapping()
    public String constructionInfo() {
        return prefix + "/constructionInfo";
    }

    @ApiOperation("根据条件查询施工信息列表")
//    @RequiresPermissions("cx:constructionInfo:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ConstructionInfoDto dto) {
        return iCxConstructionInfoService.listConstructionInfo(dto);
    }

    @ApiOperation("跳转到施工信息新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("constructionInfo", new ConstructionInfoDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取施工信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("constructionInfo", iCxConstructionInfoService.getConstructionInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改施工信息(id为空则进行新增，id不为空则进行修改)")
//    @RequiresPermissions("cx:constructionInfo:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveConstructionInfo(ConstructionInfoDto dto) {
        dto.setTireFabric1Version(dto.getEmbryoVersion());
        dto.setTireFabric2Version(dto.getEmbryoVersion());
        dto.setTireFabric3Version(dto.getEmbryoVersion());
        dto.setCordVersion(dto.getEmbryoVersion());
        dto.setInsideVersion(dto.getEmbryoVersion());
        dto.setSidewallVersion(dto.getEmbryoVersion());
        dto.setBeadVersion(dto.getEmbryoVersion());
        dto.setTireRingVersion(dto.getEmbryoVersion());
        dto.setBelt1Version(dto.getEmbryoVersion());
        dto.setBelt2Version(dto.getEmbryoVersion());
        dto.setArticleCrownVersion(dto.getEmbryoVersion());
        dto.setTreadVersion(dto.getEmbryoVersion());
        return iCxConstructionInfoService.saveConstructionInfo(dto);
    }

    @ApiOperation("校验胎胚代码唯一性")
    @PostMapping("/checkEmbryoCodeUnique")
    @ResponseBody
    public AjaxResult checkEmbryoCodeUnique(ConstructionInfoDto dto) {
        if(UserConstants.NOT_UNIQUE.equals(iCxConstructionInfoService.checkEmbryoCodeUnique(dto))){
            return AjaxResult.error(I18nUtil.getMessage("ui.construction.isEmbryoCodeExist"));
        }
        return AjaxResult.success();
    }

    @ApiOperation("刪除施工信息")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxConstructionInfoService.deleteConstructionInfo(arr);
    }

    @ApiOperation("导出施工信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, ConstructionInfoDto dto) throws IOException {
        List<ConstructionInfoDto> list = iCxConstructionInfoService.exportData(dto);
        ExcelUtil<ConstructionInfoDto> util = new ExcelUtil(ConstructionInfoDto.class);
        String fileName = I18nUtil.getMessage("ui.construction.modalName");
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
        String fileName = I18nUtil.getMessage("ui.construction.modalName");
        ExcelUtil.setResponseHeader(response, fileName);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "cx/" + fileName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        FileUtils.writeInputStream(in, response.getOutputStream());
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:constructionInfo:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.construction.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<ConstructionInfoDto> util = new ExcelUtil<>(ConstructionInfoDto.class);
        List<ConstructionInfoDto> list = util.importExcel(in, 1);
        AjaxResult ajaxResult = iCxConstructionInfoService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
