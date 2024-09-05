package com.zlt.aps.cx.service.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.cx.api.domain.dto.CxCheckConstructionResultDto;
import com.zlt.aps.cx.api.domain.entity.CxCheckConstruction;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cx.mapper.CxCheckConstructionMapper;
import com.zlt.aps.cx.service.CxCheckConstructionService;

/**
 * 施工信息检测Service业务层处理
 * 
 * @author Gim
 * @date 2022-03-09
 */
@Service
public class CxCheckConstructionServiceImpl implements CxCheckConstructionService {
	@Autowired
	private CxCheckConstructionMapper cxCheckConstructionMapper;

	private static Map<String, Field> constructionFieldsMap = null;

	/**
	 * 查询施工信息检测列表
	 * 
	 * @param cxCheckConstruction 施工信息检测
	 * @return 施工信息检测
	 */
	@Override
	public List<CxCheckConstruction> selectCxCheckConstructionList(CxCheckConstruction dispatcherLog) {
		return cxCheckConstructionMapper.selectCxCheckConstructionList(dispatcherLog);
	}

	/**
	 * 新增施工信息检测
	 * 
	 * @param cxCheckConstruction 施工信息检测
	 * @return 结果
	 */
	@Override
	public int insertCxCheckConstruction(CxCheckConstruction cxCheckConstruction) {
		cxCheckConstruction.setBaseVale(null);
		return cxCheckConstructionMapper.insertCxCheckConstruction(cxCheckConstruction);
	}

	/**
	 * 检查施工信息完整性
	 * 
	 * @param embryoCode 胎胚号
	 * @param bomVersion 施工版本
	 * @return 检查结果
	 */
	@Override
	public String checkConstruction(String embryoCode, String bomVersion) {
		if (StringUtils.isEmpty(embryoCode) || StringUtils.isEmpty(bomVersion)) {
			String msgTemplate = I18nUtil.getMessage("ui.data.column.construction.check.codeAndVersion.notExists");
			return StringUtils.format(msgTemplate, embryoCode, bomVersion);
		}
		List<CxProductConstructionInfo> constructionList = cxCheckConstructionMapper
				.getCheckConstructionInfo(embryoCode, bomVersion);
		CxProductConstructionInfo construction = CollectionUtil.firstElement(constructionList);
		if (construction == null) {
			String msgTemplate = I18nUtil.getMessage("ui.data.column.construction.check.codeAndVersion.notExists");
			return StringUtils.format(msgTemplate, embryoCode, bomVersion);
		}
		// 讲施工栏位缓存到map中
		this.initConstructionFieldsMap();
		CxCheckConstructionResultDto result = this.checkProductConstruction(construction);
		if (StringUtils.isNotEmpty(result.getErrorMessage())) {
			String msgTemplate = I18nUtil.getMessage("ui.data.column.construction.check.codeAndVersion.error");
			return StringUtils.format(msgTemplate, embryoCode, bomVersion, result.getErrorMessage());
		}
		return "";
	}

	/**
	 * 检查月度计划的施工信息
	 * 
	 * @param planMonth 计划月份
	 * @return
	 */
	@Override
	public List<CxCheckConstructionResultDto> checkMonthPlanConstructionList(Date planMonth) {
		String year = DateUtils.parseDateToStr("yyyy", planMonth);
		String month = DateUtils.parseDateToStr("MM", planMonth);
		List<CxProductConstructionInfo> constructionList = cxCheckConstructionMapper.listCheckConstructionInfo(year,
				month);
		if (constructionList.isEmpty()) {
			throw new RuntimeException(I18nUtil.getMessage("ui.data.column.construction.check.monthPlan.notExists"));
		}
		// 构建需导入到excel的检查结果
		return this.findErrorConstruction(constructionList);
	}

	/**
	 * 获取施工表
	 * 
	 * @return
	 */
	private synchronized void initConstructionFieldsMap() {
		if (constructionFieldsMap == null) {
			constructionFieldsMap = new ConcurrentHashMap<>();
			Field[] allFields = CxProductConstructionInfo.class.getDeclaredFields();
			for (int col = 0; col < allFields.length; col++) {
				Field field = allFields[col];
				Excel attr = field.getAnnotation(Excel.class);
				if (attr != null) {
					constructionFieldsMap.put(field.getName(), field);
				}
			}
		}
	}

	/**
	 * 查找异常数据
	 * 
	 * @param constructionList 待检查的施工列表
	 * @return
	 */
	private List<CxCheckConstructionResultDto> findErrorConstruction(List<CxProductConstructionInfo> constructionList) {
		List<CxCheckConstructionResultDto> resultList = new ArrayList<>();
		// 讲施工栏位缓存到map中
		this.initConstructionFieldsMap();
		for (CxProductConstructionInfo construction : constructionList) {
			// 检查施工信息
			CxCheckConstructionResultDto result = this.checkProductConstruction(construction);
			if (StringUtils.isNotEmpty(result.getErrorMessage())) {
				resultList.add(result);
			}
		}
		// 对数据排序
		List<CxCheckConstructionResultDto> sortList = resultList.stream()
				.sorted(Comparator.comparing(CxCheckConstructionResultDto::getSapCode)).collect(Collectors.toList());
		return sortList;
	}

	/**
	 * 检查施工信息
	 * 
	 * @param construction
	 * @return
	 */
	private CxCheckConstructionResultDto checkProductConstruction(CxProductConstructionInfo construction) {
		CxCheckConstructionResultDto result = new CxCheckConstructionResultDto();
		result.setEmbryoCode(construction.getEmbryoCode());
		result.setEmbryoVersion(construction.getEmbryoVersion());
		result.setSapCode(construction.getSapCode());
		// 异常信息
		StringBuffer errorMessage = new StringBuffer();
		// 先判断施工是否存在
		if (construction.getId() == null) {
			// 如果施工不存在，则直接反馈异常信息
			errorMessage.append(I18nUtil.getMessage("ui.data.column.construction.check.construction.notExists"));
		} else {
			// 判断以下所有栏位是否有缺失
			this.checkField("sapCode", construction, errorMessage);
			this.checkField("embryoCode", construction, errorMessage);
			this.checkField("specDesc", construction, errorMessage);
			this.checkField("noseWidth", construction, errorMessage);
			if (construction.getEmbryoCode().startsWith("E")) {
				// 如果胎胚是二次发，则扣圈盘直径必须不为空
				this.checkField("flipDiscDiameter", construction, errorMessage);
			}
			this.checkField("treadCode", construction, errorMessage);
			this.checkField("treadSap", construction, errorMessage);
			this.checkField("treadVersion", construction, errorMessage);
			this.checkField("treadRubberCategory", construction, errorMessage);
			this.checkField("treadMouthPlate", construction, errorMessage);
			this.checkField("treadShoulderLength", construction, errorMessage);
			this.checkField("sidewallCode", construction, errorMessage);
			this.checkField("sidewallSap", construction, errorMessage);
			this.checkField("sidewallVersion", construction, errorMessage);
			this.checkField("sidewallRubber", construction, errorMessage);
			this.checkField("sidewallMouthPlate", construction, errorMessage);
			this.checkField("sidewallLength", construction, errorMessage);
			this.checkField("insideCode", construction, errorMessage);
			this.checkField("insideSap", construction, errorMessage);
			this.checkField("insideVersion", construction, errorMessage);
			this.checkField("insideRubber", construction, errorMessage);
			this.checkField("tireRingCode", construction, errorMessage);
			this.checkField("tireRingSap", construction, errorMessage);
			this.checkField("tireRingVersion", construction, errorMessage);
			this.checkField("apexCode", construction, errorMessage);
			this.checkField("hexagonRubberCode", construction, errorMessage);
			this.checkField("hexagonMouthPlate", construction, errorMessage);
			this.checkField("beadCode", construction, errorMessage);
			this.checkField("beadSap", construction, errorMessage);
			this.checkField("beadVersion", construction, errorMessage);
			this.checkField("beadType", construction, errorMessage);
			this.checkField("beadArrange", construction, errorMessage);
			this.checkField("fitDrumPerimeter", construction, errorMessage);
			this.checkField("beltCuttingAngle", construction, errorMessage);
			this.checkField("beltCode1", construction, errorMessage);
			this.checkField("beltSap1", construction, errorMessage);
			this.checkField("belt1Version", construction, errorMessage);
			this.checkField("beltCraft1", construction, errorMessage);
			this.checkField("beltCode2", construction, errorMessage);
			this.checkField("beltSap2", construction, errorMessage);
			this.checkField("belt2Version", construction, errorMessage);
			this.checkField("beltCraft2", construction, errorMessage);
			this.checkField("articleCrownSpec", construction, errorMessage);
			this.checkField("articleCrownSap", construction, errorMessage);
			this.checkField("articleCrownVersion", construction, errorMessage);
			this.checkField("tireFabricCode1", construction, errorMessage);
			this.checkField("tireFabricSap1", construction, errorMessage);
			this.checkField("tireFabric1Version", construction, errorMessage);
			this.checkField("tireFabricCraft1", construction, errorMessage);
			if (StringUtils.isNotEmpty(construction.getTireFabricCode2())) {
				// 如果有2号胎体布，则必须有2号胎胚布工艺
				this.checkField("tireFabricCraft2", construction, errorMessage);
			}
			if (StringUtils.isNotEmpty(construction.getTireFabricCode3())) {
				// 如果有3号胎体布，则必须有3号胎胚布工艺
				this.checkField("tireFabricCraft3", construction, errorMessage);
			}
			this.checkField("cordSpec", construction, errorMessage);
			this.checkField("cordSap", construction, errorMessage);
			this.checkField("cordVersion", construction, errorMessage);
			this.checkField("originalLineCode", construction, errorMessage);
			this.checkField("delFlag", construction, errorMessage);
			this.checkField("dimension", construction, errorMessage);
			this.checkField("sectionWidth", construction, errorMessage);
			this.checkField("hexagonRubberDimension", construction, errorMessage);
			this.checkField("productionStage", construction, errorMessage);
			// 移除最后一个逗号
			if (errorMessage.length() > 0) {
				errorMessage.setLength(errorMessage.length() - 1);
			}
		}
		result.setErrorMessage(errorMessage.toString());
		return result;
	}

	/**
	 * 检查施工栏位，本栏位有错的换需要将其拼接到错误信息中
	 * 
	 * @param columnName   栏位名称
	 * @param construction 施工信息
	 * @param errorMessage 错误信息
	 */
	private void checkField(String columnName, CxProductConstructionInfo construction, StringBuffer errorMessage) {
		// 错误栏位
		String errorFeild = null;
		// 取出缓存的栏位field
		Field field = constructionFieldsMap.get(columnName);
		if (field != null) {
			// 执行栏位的getter方法，获取到单位值
			Object val = ReflectUtils.invokeGetter(construction, field.getName());
			if (val == null) {
				// 如果值为空，则该栏位为错位u栏位
				Excel annotation = field.getAnnotation(Excel.class);
				if (annotation != null && StringUtils.isNotBlank(annotation.name())) {
					errorFeild = I18nUtil.getMessage(annotation.name());
				}
			}
		}
		// 如果有错误栏位，则讲其拼接至错误信息中
		if (StringUtils.isNotEmpty(errorFeild)) {
			errorMessage.append(errorFeild).append("，");
		}
	}

}
