package com.zlt.mix.controller.setting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.GlueCommonDemand;
import com.zlt.mix.setting.api.service.IGlueCommonDemandService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 密炼机常用大规格设置Controller
 * 
 * @author zlt
 * @date 2023-02-05
 */
@Api(tags = "密炼机常用大规格设置")
@Controller
@RequestMapping("/setting/glueCommonDemand")
public class GlueCommonDemandController extends BaseController {

	@Resource
	private IGlueCommonDemandService iGlueCommonDemandService;
	@Resource
	private IExportLogService iExportLogService;
	@Resource
	private IImportErrorLogService iImportErrorLogService;
	@Resource
	private IImportLogService iImportLogService;

	private final String prefix = "setting/glueCommonDemand";

	/**
	 * 跳转至主页面
	 */
	@RequiresPermissions("setting:glueCommonDemand:view")
	@GetMapping()
	public String toIndex() {
		return prefix + "/glueCommonDemand";
	}

	@ApiOperation("根据条件查询密炼机常用大规格设置列表")
	@RequiresPermissions("setting:glueCommonDemand:list")
	@PostMapping("/list")
	@ResponseBody
	public TableDataInfo listGlueCommonDemand(GlueCommonDemand entity) {
		return iGlueCommonDemandService.listGlueCommonDemand(entity);
	}

	/**
	 * 跳转至新增页面
	 */
	@ApiOperation("跳转至新增页面")
	@GetMapping("/add")
	public String toAdd(ModelMap mmap) {
		mmap.put("glueCommonDemand", new GlueCommonDemand());
		return prefix + "/edit";
	}

	@ApiOperation("跳转至修改页面")
	@GetMapping("/edit/{id}")
	public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
		mmap.put("glueCommonDemand", iGlueCommonDemandService.getGlueCommonDemandInfo(id));
		return prefix + "/edit";
	}

	@ApiOperation("修改或新增密炼机常用大规格设置")
	@PostMapping("/save")
	@ResponseBody
	public AjaxResult saveGlueCommonDemand(GlueCommonDemand glueCommonDemand) {
		return iGlueCommonDemandService.saveGlueCommonDemand(glueCommonDemand);
	}

	@ApiOperation("删除密炼机常用大规格设置（id不为空）")
	@RequiresPermissions("setting:glueCommonDemand:remove")
	@PostMapping("/remove")
	@ResponseBody
	public AjaxResult removeGlueCommonDemand(String ids) {
		Long[] arr = Convert.toLongArray(ids);
		return iGlueCommonDemandService.deleteGlueCommonDemand(arr);
	}

	@ApiOperation("校验密炼机常用大规格设置唯一性")
	@PostMapping("/checkGlueCommonDemandUnique")
	@ResponseBody
	public String checkGlueCommonDemandUnique(GlueCommonDemand glueCommonDemand) {
		return iGlueCommonDemandService.checkGlueCommonDemandUnique(glueCommonDemand);
	}

	/**
	 * 导出密炼机常用大规格设置
	 */
	@ApiOperation("导出密炼机常用大规格设置")
	@RequiresPermissions("setting:glueCommonDemand:export")
	@GetMapping("/export")
	@ResponseBody
	public void export(HttpServletResponse response, GlueCommonDemand glueCommonDemand) throws IOException {
		String fileName = I18nUtil.getMessage("setting.glueCommonDemand.modelName");
		List<GlueCommonDemand> list = iGlueCommonDemandService.exportData(glueCommonDemand);
		ExcelUtil<GlueCommonDemand> util = new ExcelUtil<>(GlueCommonDemand.class);
		Workbook workbook = util.exportExcel2(response, list, fileName);
		ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, glueCommonDemand.toString(),
				ZltConstant.PROCEDURE_CODE_SETTING);
		iExportLogService.add(exportLog);
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
		String fileName = I18nUtil.getMessage("setting.glueCommonDemand.modelName");
		ExcelUtil<GlueCommonDemand> util = new ExcelUtil<>(GlueCommonDemand.class);
		util.exportExcel(response, null, fileName, fileName);
		return AjaxResult.success();
	}

	/**
	 * excel数据导入
	 *
	 * @param file          要导入的文件
	 * @param updateSupport 已存在的记录是否更新
	 * @return 结果
	 * @throws Exception 异常
	 */
	@RequiresPermissions("setting:glueCommonDemand:import")
	@ApiOperation("excel数据导入")
	@PostMapping("/importData")
	@ResponseBody
	public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
		// 文件解密
		byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
		ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
				I18nUtil.getMessage("setting.glueCommonDemand.modelName"), file.getOriginalFilename());
		importLog = iImportLogService.add(importLog);

		// 文件解析
		InputStream in = new ByteArrayInputStream(data);
		ExcelUtil<GlueCommonDemand> util = new ExcelUtil<>(GlueCommonDemand.class);
		List<GlueCommonDemand> list = util.importExcel(in);
		// 导入数据
		AjaxResult ajaxResult = iGlueCommonDemandService.importData(list, updateSupport, importLog.getId());
		// 更新日志记录成功数，失败数
		ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
		// 保存失败记录
		ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
		return ajaxResult;
	}

}
