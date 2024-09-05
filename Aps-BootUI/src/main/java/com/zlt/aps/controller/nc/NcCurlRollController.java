package com.zlt.aps.controller.nc;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

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
import com.zlt.aps.template.nc.NcCurlRollTemp;
import com.zlt.aps.nc.api.domain.dto.NcCurlRollDto;
import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;
import com.zlt.aps.nc.api.service.INcCurlRollService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"内衬卷曲信息接口"})
@Controller
@RequestMapping("/nc/curlRoll")
public class NcCurlRollController extends BaseController {

    private String prefix = "nc/curlRoll";

    @Resource
    private INcCurlRollService iNcCurlRollService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    @RequiresPermissions("nc:curlRoll:view")
    @GetMapping()
    public String curlRoll() {
        return prefix + "/curlRoll";
    }

    @ApiOperation("根据条件查询内衬卷曲列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcCurlRoll dto) {
        return iNcCurlRollService.listCurlRoll(dto);
    }

    @ApiOperation("跳转到内衬卷曲新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("curlRoll", new NcCurlRoll());
        return prefix + "/edit";
    }

    @ApiOperation("获取内衬卷曲信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("curlRoll", iNcCurlRollService.getCurlRoll(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改内衬卷曲(id为空则进行新增，id不为空则进行修改)")
//    @RequiresPermissions("nc:curlRoll:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveCurlRoll(NcCurlRoll dto) {
        return iNcCurlRollService.saveCurlRoll(dto);
    }

    @ApiOperation("根据code判断内衬卷曲代号是否已经存在")
    @PostMapping("/checkCurlRollCodeUnique")
    @ResponseBody
    public String checkCurlRollCodeUnique(NcCurlRoll dto) {
        return iNcCurlRollService.checkCurlRollCodeUnique(dto);
    }

    @ApiOperation("刪除内衬卷曲")
//    @RequiresPermissions("nc:curlRoll:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcCurlRollService.deleteCurlRoll(arr);
    }

    @ApiOperation("导出内衬卷曲")
    @GetMapping("/export")
    @ResponseBody
	public void export(HttpServletResponse response, NcCurlRoll dto) throws IOException {
    	// 查询数据，并转换成导出对象
		List<NcCurlRollTemp> exportList = iNcCurlRollService.exportData(dto).stream().map(item -> {
			NcCurlRollTemp expotItem = new NcCurlRollTemp();
			expotItem.setCurlLength(item.getCurlLength());
			expotItem.setLiningCode(item.getLiningCode());
			expotItem.setRemark(item.getRemark());
			return expotItem;
		}).collect(Collectors.toList());
		ExcelUtil<NcCurlRollTemp> util = new ExcelUtil(NcCurlRollTemp.class);
		String fileName = I18nUtil.getMessage("ui.nc.curlRoll.column.modalName");
		Workbook workbook = util.exportExcel2(response, exportList, fileName);
		ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_NC);
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
        String fileName = I18nUtil.getMessage("ui.nc.curlRoll.column.modalName");
        ExcelUtil<NcCurlRollTemp> util = new ExcelUtil<>(NcCurlRollTemp.class);
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
    @RequiresPermissions("nc:curlRoll:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_NC, I18nUtil.getMessage("ui.nc.curlRoll.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<NcCurlRollDto> util = new ExcelUtil<>(NcCurlRollDto.class);
    	// 将导入对象转换成实体对象
		List<NcCurlRollDto> importList = util.importExcel(in);
        
        AjaxResult ajaxResult = iNcCurlRollService.importData(importList, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
