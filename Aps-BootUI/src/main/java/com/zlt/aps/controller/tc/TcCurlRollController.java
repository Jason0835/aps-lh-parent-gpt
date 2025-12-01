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
import com.zlt.aps.tc.api.domain.dto.TcCurlRollDto;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;
import com.zlt.aps.tc.api.service.ITcCurlRollService;
import com.zlt.aps.template.tc.TcCurlRollTemp;
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

@Api(tags = {"胎侧卷曲信息接口"})
@Controller
@RequestMapping("/tc/curlRoll")
public class TcCurlRollController extends BaseController {

    private String prefix = "tc/curlRoll";

    @Resource
    private ITcCurlRollService iTcCurlRollService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    @RequiresPermissions("tc:curlRoll:view")
    @GetMapping()
    public String curlRoll() {
        return prefix + "/curlRoll";
    }

    @ApiOperation("根据条件查询胎侧卷曲列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcCurlRoll dto) {
        return iTcCurlRollService.listCurlRoll(dto);
    }

    @ApiOperation("跳转到胎侧卷曲新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("curlRoll", new TcCurlRoll());
        return prefix + "/edit";
    }

    @ApiOperation("获取胎侧卷曲信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("curlRoll", iTcCurlRollService.getCurlRoll(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改胎侧卷曲(id为空则进行新增，id不为空则进行修改)")
//    @RequiresPermissions("tc:curlRoll:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveCurlRoll(TcCurlRoll dto) {
        return iTcCurlRollService.saveCurlRoll(dto);
    }

    @ApiOperation("根据code判断胎侧卷曲代号是否已经存在")
    @PostMapping("/checkCurlRollCodeUnique")
    @ResponseBody
    public String checkCurlRollCodeUnique(TcCurlRoll dto) {
        return iTcCurlRollService.checkCurlRollCodeUnique(dto);
    }

    @ApiOperation("刪除胎侧卷曲")
//    @RequiresPermissions("tc:curlRoll:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcCurlRollService.deleteCurlRoll(arr);
    }

    @ApiOperation("导出胎侧卷曲")
    @GetMapping("/export")
    @ResponseBody
	public void export(HttpServletResponse response, TcCurlRoll dto) throws IOException {
    	// 查询数据，并转换成导出对象
		List<TcCurlRollTemp> exportList = iTcCurlRollService.exportData(dto).stream().map(item -> {
			TcCurlRollTemp expotItem = new TcCurlRollTemp();
			expotItem.setCurlLength(item.getCurlLength());
			expotItem.setSidewallCode(item.getSidewallCode());
			expotItem.setRemark(item.getRemark());
			return expotItem;
		}).collect(Collectors.toList());
		ExcelUtil<TcCurlRollTemp> util = new ExcelUtil(TcCurlRollTemp.class);
		String fileName = I18nUtil.getMessage("ui.tc.curlRoll.column.modalName");
		Workbook workbook = util.exportExcel2(response, exportList, fileName);
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
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.tc.curlRoll.column.modalName");
        ExcelUtil<TcCurlRollTemp> util = new ExcelUtil<>(TcCurlRollTemp.class);
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
    @RequiresPermissions("tc:curlRoll:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TC, I18nUtil.getMessage("ui.tc.curlRoll.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<TcCurlRollDto> util = new ExcelUtil<>(TcCurlRollDto.class);
    	// 将导入对象转换成实体对象
		List<TcCurlRollDto> importList = util.importExcel(in);

        AjaxResult ajaxResult = iTcCurlRollService.importData(importList, updateSupport, importLog.getId());
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
    public AjaxResult selectCurlLengthByCode(TcCurlRoll curlRoll) {
        return iTcCurlRollService.selectCurlLengthByCode(curlRoll);
    }

}
