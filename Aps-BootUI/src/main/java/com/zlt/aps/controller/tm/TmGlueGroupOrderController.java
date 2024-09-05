package com.zlt.aps.controller.tm;


import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.tm.TmGlueGroupOrderTemp;
import com.zlt.aps.tm.api.domain.dto.TmGlueGroupOrderDto;
import com.zlt.aps.tm.api.service.ITmGlueGroupOrderService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Api(tags = {"胎面胶料组别顺序维护接口"})
@Controller
@RequestMapping("/tm/glueGroupOrder")
public class TmGlueGroupOrderController extends BaseController {

    private String prefix = "tm/glueGroupOrder";

    @Resource
    private ITmGlueGroupOrderService iTmGlueGroupOrderService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @RequiresPermissions("tm:glueGroupOrder:view")
    @GetMapping()
    public String glueGroupOrder() {
        return prefix + "/glueGroupOrder";
    }

    @ApiOperation("根据条件查询胶料组别顺序列表")
    @RequiresPermissions("tm:glueGroupOrder:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmGlueGroupOrderDto dto) {
        return iTmGlueGroupOrderService.listGlueGroupOrder(dto);
    }

    @ApiOperation("跳转到胶料组别顺序新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("glueGroupOrder", new TmGlueGroupOrderDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取胶料组别顺序信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("glueGroupOrder", iTmGlueGroupOrderService.getGlueGroupOrder(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改胶料组别顺序(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions("tm:glueGroupOrder:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueGroupOrder(TmGlueGroupOrderDto dto, HttpServletRequest request) {
        String aa = SecurityUtils.getUsername();
        String aa1 = AuthorizationUtils.getLoginName();
        String aa12 = AuthorizationUtils.getDepartName();
        System.getenv("MVNW_USERNAME");
        return iTmGlueGroupOrderService.saveGlueGroupOrder(dto);
    }

    @ApiOperation("根据code判断胶料组号是否已经存在")
    @PostMapping("/checkGlueGroupCodeUnique")
    @ResponseBody
    public String checkGlueGroupCodeUnique(TmGlueGroupOrderDto dto) {
        return iTmGlueGroupOrderService.checkGlueGroupCodeUnique(dto);
    }

    @ApiOperation("刪除胶料组别顺序")
    @RequiresPermissions("tm:glueGroupOrder:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTmGlueGroupOrderService.deleteGlueGroupOrder(arr);
    }

    @ApiOperation("导出胶料组别顺序")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TmGlueGroupOrderDto dto) throws IOException {
        String fileName = I18nUtil.getMessage("ui.tm.glueGroup.column.modalName");
        List<TmGlueGroupOrderDto> list = iTmGlueGroupOrderService.exportData(dto);
        ExcelUtil<TmGlueGroupOrderDto> util = new ExcelUtil(TmGlueGroupOrderDto.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TM);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.tm.glueGroup.column.modalName");
        ExcelUtil<TmGlueGroupOrderTemp> util = new ExcelUtil<>(TmGlueGroupOrderTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("tm:glueGroupOrder:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TM,
                I18nUtil.getMessage("ui.tm.glueGroup.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TmGlueGroupOrderDto> util = new ExcelUtil<>(TmGlueGroupOrderDto.class);
        List<TmGlueGroupOrderDto> list = util.importExcel(in);
        // 导入数据
        AjaxResult ajaxResult = iTmGlueGroupOrderService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        //保存导入错误日志信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
