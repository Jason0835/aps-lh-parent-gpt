package com.zlt.aps.controller.cd90;


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
import com.zlt.aps.cd90.api.domain.dto.Cd90BigRollDto;
import com.zlt.aps.cd90.api.service.ICd90BigRollService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd90.Cd90BigRollTemp;
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

@Api(tags = {"90度裁断帘布大卷接口"})
@Controller
@RequestMapping("/cd90/bigRoll")
public class Cd90BigRollController extends BaseController {

    private String prefix = "cd90/bigRoll";

    @Resource
    private ICd90BigRollService iCd90BigRollService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    @RequiresPermissions("cd90:bigRoll:view")
    @GetMapping()
    public String bigRoll() {
        return prefix + "/bigRoll";
    }

    @ApiOperation("根据条件查询帘布大卷列表")
//    @RequiresPermissions("cd90:bigRoll:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90BigRollDto dto) {
        return iCd90BigRollService.listBigRoll(dto);
    }

    @ApiOperation("跳转到帘布大卷新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("bigRoll", new Cd90BigRollDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取帘布大卷信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("bigRoll", iCd90BigRollService.getBigRoll(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改帘布大卷(id为空则进行新增，id不为空则进行修改)")
//    @RequiresPermissions("cd90:bigRoll:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveBigRoll(Cd90BigRollDto dto) {
        return iCd90BigRollService.saveBigRoll(dto);
    }

    @ApiOperation("根据code判断帘布大卷代号是否已经存在")
    @PostMapping("/checkBigRollCodeUnique")
    @ResponseBody
    public String checkBigRollCodeUnique(Cd90BigRollDto dto) {
        return iCd90BigRollService.checkBigRollCodeUnique(dto);
    }

    @ApiOperation("刪除帘布大卷")
//    @RequiresPermissions("cd90:bigRoll:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCd90BigRollService.deleteBigRoll(arr);
    }

    @ApiOperation("导出帘布大卷")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd90BigRollDto dto) throws IOException {
        List<Cd90BigRollDto> list = iCd90BigRollService.exportData(dto);
        ExcelUtil<Cd90BigRollDto> util = new ExcelUtil(Cd90BigRollDto.class);
        String fileName = I18nUtil.getMessage("ui.cd90.bigRoll.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CD90);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cd90.bigRoll.column.modalName");
        ExcelUtil<Cd90BigRollTemp> util = new ExcelUtil<>(Cd90BigRollTemp.class);
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
    @RequiresPermissions("cd90:bigRoll:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD90, I18nUtil.getMessage("ui.cd90.bigRoll.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<Cd90BigRollDto> util = new ExcelUtil<>(Cd90BigRollDto.class);
        List<Cd90BigRollDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iCd90BigRollService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
