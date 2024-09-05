package com.zlt.aps.controller.cx;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.utils.StringUtils;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.ruoyi.file.api.service.IApsFileService;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.cx.api.domain.entity.CxCheckConstruction;
import com.zlt.aps.cx.api.service.ICheckConstructionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.Date;

/**
 * 施工信息检测接口
 */
@Controller
@RequestMapping("/cx/checkConstruction")
@Api(tags = { "施工信息检测接口" })
public class CxCheckConstructionController extends BaseController {
	private final String prefix = "cx/constructionCheck";

	@Autowired
	private ICheckConstructionService iCheckConstructionService;

	@Autowired
	private IApsFileService iApsFileService;

	@RequiresPermissions("cx:checkConstruction:view")
	@GetMapping()
	public String constructionCheck(ModelMap mmap) {
		mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM", new Date()));
		return prefix + "/constructionCheck";
	}

	/**
	 * 查询月度检测施工
	 */
	@RequiresPermissions("cx:checkConstruction:list")
	@ApiOperation("查询月度检测施工")
	@PostMapping("/list")
	@ResponseBody
	public TableDataInfo list(CxCheckConstruction cxCheckConstruction) {
		return iCheckConstructionService.list(cxCheckConstruction);
	}

	/**
	 * 检测施工
	 */
	@ApiOperation("检测施工")
	@RequiresPermissions("cx:checkConstruction:checkConstruction")
	@PostMapping("/checkConstruction")
	@ResponseBody
	public AjaxResult checkConstruction(CxCheckConstruction cxCheckConstruction) throws Exception {
		if (cxCheckConstruction == null || cxCheckConstruction.getPlanMonth() == null) {
			return AjaxResult.error(I18nUtil.getMessage("ui.data.column.construction.check.planMonth.isNull"));
		}
		CxCheckConstruction result = iCheckConstructionService
				.buildCheckConstructionExcel(cxCheckConstruction);
		byte[] datas = result.getFileData();
		if (datas != null) {
			String filePath = ExportUtil.uploadExcelByByte(datas);
			if (StringUtils.isBlank(filePath)) {
				return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
			}
			cxCheckConstruction.setFilePath(filePath);
			// 拼接生成的excel文件名称
			String fileName = StringUtils.join(I18nUtil.getMessage("ui.data.column.construction.check.checkResult.name"),
					"-", DateUtils.dateTimeNow(), ".xlsx");
			cxCheckConstruction.setFileName(fileName);
		}
		cxCheckConstruction.setIsComplete(result.getIsComplete());
		return iCheckConstructionService.saveCheckConstruction(cxCheckConstruction);
	}

	/**
	 * 下载文件
	 * 
	 * @param name
	 * @param url
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@GetMapping("/download")
	public void resourceDownload(String name, String url, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		response.setCharacterEncoding("utf-8");
		response.setContentType("multipart/form-data");
		response.setHeader("Content-Disposition",
				"attachment;fileName=" + FileUtils.setFileDownloadHeader(request, name));
		byte[] data = iApsFileService.downloadByteFile(url, "export");
		OutputStream outputStream = response.getOutputStream();
		IOUtils.write(data, outputStream);
		outputStream.close();
	}
}