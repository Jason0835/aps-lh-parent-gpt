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
import com.zlt.aps.gsq.api.domain.dto.GsqTwiningDiscDto;
import com.zlt.aps.gsq.api.service.IGsqTwiningDiscService;
import com.zlt.aps.template.gsq.GsqTwiningDiscTemp;
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

@Api(tags = {"钢丝圈缠绕盘维护接口"})
@Controller
@RequestMapping("/gsq/twiningDisc")
public class GsqTwiningDiscController extends BaseController {

    private String prefix = "gsq/twiningDisc";

    @Resource
    private IGsqTwiningDiscService iGsqTwiningDiscService;

    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    @RequiresPermissions("gsq:twiningDisc:view")
    @GetMapping()
    public String TwiningDisc() {
        return prefix + "/twiningDisc";
    }

    @ApiOperation("根据条件查询缠绕盘列表")
    @RequiresPermissions("gsq:twiningDisc:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqTwiningDiscDto dto) {
        return iGsqTwiningDiscService.listTwiningDisc(dto);
    }

    @ApiOperation("跳转到缠绕盘新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("twiningDisc", new GsqTwiningDiscDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取缠绕盘信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("twiningDisc", iGsqTwiningDiscService.getTwiningDisc(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改缠绕盘(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions("gsq:twiningDisc:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveTwiningDisc(GsqTwiningDiscDto dto) {
        return iGsqTwiningDiscService.saveTwiningDisc(dto);
    }

    @ApiOperation("判断缠绕code是否唯一")
    @PostMapping("/checkSerialNumberUnique")
    @ResponseBody
    public String checkSerialNumberUnique(GsqTwiningDiscDto dto) {
        return iGsqTwiningDiscService.checkSerialNumberUnique(dto);
    }

    @ApiOperation("刪除缠绕盘")
    @RequiresPermissions("gsq:twiningDisc:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGsqTwiningDiscService.deleteTwiningDisc(arr);
    }


    @ApiOperation("刪除全部")
    @RequiresPermissions("gsq:twiningDisc:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iGsqTwiningDiscService.deleteAll();
    }


    @RequiresPermissions("gsq:twiningDisc:export")
    @ApiOperation("导出缠绕盘")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GsqTwiningDiscDto dto) throws IOException {
        List<GsqTwiningDiscDto> list = iGsqTwiningDiscService.exportData(dto);
        ExcelUtil<GsqTwiningDiscDto> util = new ExcelUtil(GsqTwiningDiscDto.class);
        String fileName = I18nUtil.getMessage("ui.gsq.twiningDisc.column.modalName");
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
        String fileName = I18nUtil.getMessage("ui.gsq.twiningDisc.column.modalName");
        ExcelUtil<GsqTwiningDiscTemp> util = new ExcelUtil<>(GsqTwiningDiscTemp.class);
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
    @RequiresPermissions("gsq:twiningDisc:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GSQ,
                I18nUtil.getMessage("ui.gsq.twiningDisc.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<GsqTwiningDiscDto> util = new ExcelUtil<>(GsqTwiningDiscDto.class);
        List<GsqTwiningDiscDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iGsqTwiningDiscService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
