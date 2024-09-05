package com.zlt.aps.controller.tq;


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
import com.zlt.aps.template.tq.TqMouthPlateTemp;
import com.zlt.aps.tq.api.domain.dto.TqMouthPlateDto;
import com.zlt.aps.tq.api.service.ITqMouthPlateService;
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
import java.util.List;

/**
 * <p>
 * 胎圈口型板信息维护 前端控制器
 * </p>
 *
 * @author chen
 * @since 2021-06-08
 */
@Controller
@RequestMapping("/tq/mouthPlate")
@Api(tags = {"胎圈口型板信息接口"})
public class TqMouthPlateController extends BaseController {

    private final String prefix = "tq/mouthPlate";

    @Autowired
    private ITqMouthPlateService iTqMouthPlateService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    @RequiresPermissions("tq:mouthPlate:view")
    @GetMapping()
    @ApiOperation("跳转到口型板信息首页")
    public String toIndex() {
        return prefix + "/mouthPlate";
    }

    /**
     * 查询胎圈口型板信息维护列表
     */
    @RequiresPermissions("tq:mouthPlate:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询胎圈口型板信息维护列表")
    public TableDataInfo list(TqMouthPlateDto dto) {
        return iTqMouthPlateService.list(dto);
    }

    /**
     * 根据id获取胎圈口型板信息维护详细信息
     */
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎圈口型板信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("MouthPlate", iTqMouthPlateService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("跳转到胎圈口型板新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("MouthPlate", new TqMouthPlateDto());
        return prefix + "/edit";
    }

    /**
     * 保存胎圈口型板信息维护
     */
    @RequiresPermissions("tq:mouthPlate:edit")
    @PostMapping("/edit")
    @ResponseBody
    @ApiOperation("保存胎圈口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(TqMouthPlateDto dto) {
        return iTqMouthPlateService.edit(dto);
    }

    /**
     * 删除胎圈口型板信息维护
     */
    @RequiresPermissions("tq:mouthPlate:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除胎圈口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqMouthPlateService.remove(arr);
    }



    @ApiOperation("刪除全部")
    @RequiresPermissions("tq:mouthPlate:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTqMouthPlateService.deleteAll();
    }


    /**
     * 导出胎圈口型板信息
     */
    @RequiresPermissions("tq:mouthPlate:export")
    @ApiOperation("导出胎圈口型板信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TqMouthPlateDto dto) throws IOException {
        List<TqMouthPlateDto> list = iTqMouthPlateService.exportData(dto);
        ExcelUtil<TqMouthPlateDto> util = new ExcelUtil<>(TqMouthPlateDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.tq.mouthPlate.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TQ);
        iExportLogService.add(exportLog);
    }

    @ApiOperation("下载模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tq.mouthPlate.modelName");
        ExcelUtil<TqMouthPlateTemp> util = new ExcelUtil<>(TqMouthPlateTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("tq:mouthPlate:import")
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TQ,
                I18nUtil.getMessage("ui.data.column.tq.mouthPlate.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TqMouthPlateDto> util = new ExcelUtil<>(TqMouthPlateDto.class);
        List<TqMouthPlateDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iTqMouthPlateService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
