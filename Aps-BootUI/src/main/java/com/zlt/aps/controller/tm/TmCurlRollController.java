package com.zlt.aps.controller.tm;


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
import com.zlt.aps.template.tm.TmCurlRollTemp;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;
import com.zlt.aps.tm.api.service.ITmCurlRollService;
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
import java.util.stream.Collectors;

@Api(tags = {"胎面卷曲信息接口"})
@Controller
@RequestMapping("/tm/curlRoll")
public class TmCurlRollController extends BaseController {

    private String prefix = "tm/curlRoll";

    @Resource
    private ITmCurlRollService iTmCurlRollService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    @RequiresPermissions("tm:curlRoll:view")
    @GetMapping()
    public String curlRoll() {
        return prefix + "/curlRoll";
    }

    @ApiOperation("根据条件查询胎面卷曲列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmCurlRoll dto) {
        return iTmCurlRollService.listCurlRoll(dto);
    }

    @ApiOperation("跳转到胎面卷曲新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("curlRoll", new TmCurlRoll());
        return prefix + "/edit";
    }

    @ApiOperation("获取胎面卷曲信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("curlRoll", iTmCurlRollService.getCurlRoll(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改胎面卷曲(id为空则进行新增，id不为空则进行修改)")
//    @RequiresPermissions("tm:curlRoll:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveCurlRoll(TmCurlRoll dto) {
        return iTmCurlRollService.saveCurlRoll(dto);
    }

    @ApiOperation("根据code判断胎面卷曲代号是否已经存在")
    @PostMapping("/checkCurlRollCodeUnique")
    @ResponseBody
    public String checkCurlRollCodeUnique(TmCurlRoll dto) {
        return iTmCurlRollService.checkCurlRollCodeUnique(dto);
    }

    @ApiOperation("刪除胎面卷曲")
//    @RequiresPermissions("tm:curlRoll:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTmCurlRollService.deleteCurlRoll(arr);
    }

    @ApiOperation("导出胎面卷曲")
    @GetMapping("/export")
    @ResponseBody
	public void export(HttpServletResponse response, TmCurlRoll dto) throws IOException {
    	// 查询数据，并转换成导出对象
		List<TmCurlRollTemp> exportList = iTmCurlRollService.exportData(dto).stream().map(item -> {
			TmCurlRollTemp expotItem = new TmCurlRollTemp();
			expotItem.setCurlLength(item.getCurlLength());
			expotItem.setTreadCode(item.getTreadCode());
			expotItem.setRemark(item.getRemark());
			return expotItem;
		}).collect(Collectors.toList());
		ExcelUtil<TmCurlRollTemp> util = new ExcelUtil(TmCurlRollTemp.class);
		String fileName = I18nUtil.getMessage("ui.tm.curlRoll.column.modalName");
		Workbook workbook = util.exportExcel2(response, exportList, fileName);
		ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TM);
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
        String fileName = I18nUtil.getMessage("ui.tm.curlRoll.column.modalName");
        ExcelUtil<TmCurlRollTemp> util = new ExcelUtil<>(TmCurlRollTemp.class);
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
    @RequiresPermissions("tm:curlRoll:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TM, I18nUtil.getMessage("ui.tm.curlRoll.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<TmCurlRollTemp> util = new ExcelUtil<>(TmCurlRollTemp.class);
    	// 将导入对象转换成实体对象
		List<TmCurlRoll> importList = util.importExcel(in).stream().map(item -> {
			TmCurlRoll expotItem = new TmCurlRoll();
			expotItem.setCurlLength(item.getCurlLength());
			expotItem.setTreadCode(item.getTreadCode());
			expotItem.setRemark(item.getRemark());
			return expotItem;
		}).collect(Collectors.toList());

        AjaxResult ajaxResult = iTmCurlRollService.importData(importList, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    @ApiOperation("根据编号查询卷曲长度")
    @PostMapping("/selectCurlLengthByCode")
    @ResponseBody
    public AjaxResult selectCurlLengthByCode(TmCurlRoll curlRoll) {
        return iTmCurlRollService.selectCurlLengthByCode(curlRoll);
    }
}
