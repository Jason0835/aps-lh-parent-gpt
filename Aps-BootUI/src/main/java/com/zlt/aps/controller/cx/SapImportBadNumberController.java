package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.SapImportBadNumber;
import com.zlt.aps.cx.api.service.ISapImportBadNumberService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * SAP导入不良数Controller
 * @author Joran.zhang
 * @date 2022-01-15
 */
@Api(tags = "SAP导入不良数")
@Controller
@RequestMapping("/cx/badNumber")
public class SapImportBadNumberController extends BaseController {

    @Autowired
    private ISapImportBadNumberService iSapImportBadNumberService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/badNumber";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:badNumber:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/badNumber";
    }

    /**
     * 根据条件查询SAP导入不良数列表
     */
    @ApiOperation("根据条件查询SAP导入不良数列表")
    @RequiresPermissions("cx:badNumber:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SapImportBadNumber entity) {
        return iSapImportBadNumberService.list(entity);
    }

    /**
     * 下载导入模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.badNumber.modelName");
        ExcelUtil<SapImportBadNumber> util = new ExcelUtil<>(SapImportBadNumber.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("cx:badNumber:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.badNumber.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<SapImportBadNumber> util = new ExcelUtil<>(SapImportBadNumber.class);
        InputStream in = new ByteArrayInputStream(data);
        List<SapImportBadNumber> list = util.importExcel(in);
        AjaxResult ajaxResult = iSapImportBadNumberService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
