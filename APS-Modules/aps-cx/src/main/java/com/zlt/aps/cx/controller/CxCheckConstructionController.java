package com.zlt.aps.cx.controller;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.annotation.Excel.Type;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.api.domain.dto.CxCheckConstructionResultDto;
import com.zlt.aps.cx.api.domain.entity.CxCheckConstruction;
import com.zlt.aps.cx.service.CxCheckConstructionService;

import io.swagger.annotations.ApiOperation;

/**
 * 施工信息检测Controller
 *
 * @author Gim
 * @date 2022-03-09
 */
@RestController
@RequestMapping("/checkConstruction")
public class CxCheckConstructionController extends BaseController {
	@Autowired
	private CxCheckConstructionService checkConstructionService;

	/**
	 * 查询施工信息检测列表
	 */
	@ApiOperation("查询施工信息检测列表")
	@PostMapping("/list")
	public TableDataInfo list(@RequestBody CxCheckConstruction checkConstruction) {
		startPage();
		checkConstruction.setOrderStr(orderStr());
		List<CxCheckConstruction> list = checkConstructionService.selectCxCheckConstructionList(checkConstruction);
		return getDataTable(list);
	}

	/**
	 * 检测施工
	 */
	@ApiOperation("检测施工")
	@PostMapping("/buildCheckConstructionExcel")
	public CxCheckConstruction buildCheckConstructionExcel(@RequestBody CxCheckConstruction cxCheckConstruction) {
		Date planMonth = cxCheckConstruction.getPlanMonth();
		// 获取指定月份施工信息的检查结果
		List<CxCheckConstructionResultDto> resultList = checkConstructionService
				.checkMonthPlanConstructionList(planMonth);
		// 封装检测结果
		CxCheckConstruction result = new CxCheckConstruction();
		boolean isComplete = true;
		if (CollectionUtils.isNotEmpty(resultList)) {
			// 构建excel
			ExcelUtil<CxCheckConstructionResultDto> excelUtil = new ExcelUtil<>(CxCheckConstructionResultDto.class);
			excelUtil.init(resultList, "Sheet1", Type.EXPORT);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			excelUtil.exportExcel(os);
			// 是否完整，通过是否包含任意异常信息
			isComplete = resultList.stream().anyMatch(r -> StringUtils.isNotEmpty(r.getErrorMessage()));
			result.setFileData(os.toByteArray());
		}
		result.setIsComplete(isComplete ? 0 : 1);
		return result;
	}

	/**
	 * 检测施工
	 */
	@ApiOperation("保存检测施工")
	@PostMapping("/saveCheckConstruction")
	public AjaxResult saveCheckConstruction(@RequestBody CxCheckConstruction cxCheckConstruction) {
		checkConstructionService.insertCxCheckConstruction(cxCheckConstruction);
		return AjaxResult.success();
	}
}
