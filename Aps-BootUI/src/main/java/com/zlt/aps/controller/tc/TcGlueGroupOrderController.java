package com.zlt.aps.controller.tc;


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
import com.zlt.aps.tc.api.domain.dto.TcGlueGroupOrderDto;
import com.zlt.aps.tc.api.service.ITcGlueGroupOrderService;
import com.zlt.aps.template.tc.TcGlueGroupOrderTemp;
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

@Api(tags = {"胎侧胶料组别顺序维护接口"})
@Controller
@RequestMapping("/tc/glueGroupOrder")
public class TcGlueGroupOrderController extends BaseController {

    private String prefix = "tc/glueGroupOrder";

    @Resource
    private ITcGlueGroupOrderService iTcGlueGroupOrderService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    @RequiresPermissions("tc:glueGroupOrder:view")
    @GetMapping()
    public String glueGroupOrder() {
        return prefix + "/glueGroupOrder";
    }

    @ApiOperation("根据条件查询胶料组别顺序列表")
    @RequiresPermissions("tc:glueGroupOrder:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcGlueGroupOrderDto dto) {
        return iTcGlueGroupOrderService.listGlueGroupOrder(dto);
    }

    @ApiOperation("跳转到胶料组别顺序新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("glueGroupOrder", new TcGlueGroupOrderDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取胶料组别顺序信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("glueGroupOrder", iTcGlueGroupOrderService.getGlueGroupOrder(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改胶料组别顺序(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions("tc:glueGroupOrder:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueGroupOrder(TcGlueGroupOrderDto dto) {
        return iTcGlueGroupOrderService.saveGlueGroupOrder(dto);
    }

    @ApiOperation("根据code判断胶料组号是否已经存在")
    @PostMapping("/checkGlueGroupCodeUnique")
    @ResponseBody
    public String checkGlueGroupCodeUnique(TcGlueGroupOrderDto dto) {
        return iTcGlueGroupOrderService.checkGlueGroupCodeUnique(dto);
    }

    @ApiOperation("刪除胶料组别顺序")
    @RequiresPermissions("tc:glueGroupOrder:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcGlueGroupOrderService.deleteGlueGroupOrder(arr);
    }

    @ApiOperation("导出胶料组别顺序")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TcGlueGroupOrderDto dto) throws IOException {
        List<TcGlueGroupOrderDto> list = iTcGlueGroupOrderService.exportData(dto);
        ExcelUtil<TcGlueGroupOrderDto> util = new ExcelUtil(TcGlueGroupOrderDto.class);
        String fileName = I18nUtil.getMessage("ui.tc.glueGroup.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TC);
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
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.tc.glueGroup.column.modalName");
        ExcelUtil<TcGlueGroupOrderTemp> util = new ExcelUtil<>(TcGlueGroupOrderTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @throws Exception
     */
    @RequiresPermissions("tc:glueGroupOrder:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TC,
                I18nUtil.getMessage("ui.tc.glueGroup.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //解析文件
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TcGlueGroupOrderDto> util = new ExcelUtil<>(TcGlueGroupOrderDto.class);
        List<TcGlueGroupOrderDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iTcGlueGroupOrderService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        //保存导入错误日志信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
