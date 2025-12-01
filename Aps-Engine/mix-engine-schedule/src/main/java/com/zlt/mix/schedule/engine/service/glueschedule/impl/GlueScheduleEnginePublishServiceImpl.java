package com.zlt.mix.schedule.engine.service.glueschedule.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.common.engine.utils.CollectionUtil;
import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.mapper.SchedulePublishEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.MachineEngineService;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEnginePublishService;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.SchedulePublishLogVo;
import com.zlt.mix.schedule.engine.vo.SchedulePublishRecordVo;
import com.zlt.mix.schedule.engine.vo.SchedulePublishVo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GlueScheduleEnginePublishServiceImpl implements GlueScheduleEnginePublishService {
	@Autowired
	private SchedulePublishEngineMapper schedulePublishEngineMapper;
	@Autowired
	private MachineEngineService machineEngineService;
	/**
	 * 接口地址
	 */
	@Value("${syncdata.webservice.url:#{null}}")
	private String pubishInterfaceUrl;
//	private String pubishInterfaceUrl="http://192.168.1.137:8018/APSServiceTest/Service.asmx";
	@Value("${syncdata.webservice.timeOut:5000}")
	private Integer connectTimeout;
	/**
	 * webservice接口返回参数项
	 */
	private static final String WS_RESULT_KEY = "APSPlanInfoResult";
	private static final String WS_RESULT = "Result";
	private static final String WS_MESSAGE = "Message";
	/**
	 * webservice接口状态码：成功
	 */
	private static final String WS_CODE_SUCCESS = "1";
	/**
	 * webservice接口状态码：失败
	 */
	private static final String WS_CODE_ERROR = "0";
	/**
	 * 超时标记
	 */
	private static final String TIMEOUT_TAG = "TIMEOUT_TAG";
	/**
	 * 工单号拼接分隔符
	 */
	private static final String ORDER_NO_SPLIT = "_";
	/**
	 * 发布类型：终炼母炼排程发布
	 */
	private static final String SCHEDULE_TYPE_GLUE = "0";
	/**
	 * 发布类型：终炼母炼排程发布
	 */
	private static final String SCHEDULE_TYPE_MATERIAL = "1";

	/**
	 * 排程发布webservice接口参数模板
	 */
	public static final String WS_PARAMS_TEMPLATE;
	static {
		// 模板在类加载时直接初始化好
		StringBuffer soap = new StringBuffer();
		soap.append(
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tem=\"http://tempuri.org/\">\r\n");
		soap.append("<soapenv:Header/><soapenv:Body>\r\n");
		soap.append("<tem:APSPlanInfo>\r\n");
		soap.append("<tem:PlanDate>{}</tem:PlanDate>\r\n");
		soap.append("<tem:shiftID>{}</tem:shiftID>\r\n");
		soap.append("<tem:EquipCode>{}</tem:EquipCode>\r\n");
		soap.append("<tem:PlanNum>{}</tem:PlanNum>\r\n");
		soap.append("<tem:MaterCode>{}</tem:MaterCode>\r\n");
		soap.append("<tem:RecipeType>{}</tem:RecipeType>\r\n");
		soap.append("<tem:EdtCode>{}</tem:EdtCode>\r\n");
		soap.append("<tem:Orderid>{}</tem:Orderid>\r\n");
		soap.append("<tem:APSCode>{}</tem:APSCode>\r\n");
		soap.append("</tem:APSPlanInfo>\r\n");
		soap.append("</soapenv:Body></soapenv:Envelope>");
		WS_PARAMS_TEMPLATE = soap.toString();
	}

	/**
	 * 机台名称列表
	 */
	private Map<String, String> machineNameMap;

	/**
	 * 向MES发布胶料排程记录
	 * 
	 * @param scheduleList
	 */
	@Override
	@Transactional
	public AjaxResult publishGlueScheduleResult(List<GlueScheduleResultVo> scheduleList) {
		// 加载机台名称
		machineNameMap = machineEngineService.mapMixMachineName(null);

		// 将硫磺辅料排程记录转换成胶料排程记录对象
		List<SchedulePublishVo> publishList = new ArrayList<>(scheduleList.size());
		List<SchedulePublishLogVo> errorRecordList = new ArrayList<>(scheduleList.size());
		List<GlueScheduleResultVo> readyPublishList = new ArrayList<>(scheduleList.size());
		for (GlueScheduleResultVo schedule : scheduleList) {
			SchedulePublishVo publishVo = new SchedulePublishVo();
			publishVo.setId(schedule.getId());
			publishVo.setMixArea(schedule.getMixArea());
			publishVo.setMachineCode(schedule.getMachineCode());
			publishVo.setScheduleDate(schedule.getScheduleDate());
			publishVo.setMidPlanQty(schedule.getMidPlanQty());
			publishVo.setMidProduceOrder(schedule.getMidProduceOrder());
			publishVo.setNightPlanQty(schedule.getNightPlanQty());
			publishVo.setNightProduceOrder(schedule.getNightProduceOrder());
			publishVo.setDayPlanQty(schedule.getDayPlanQty());
			publishVo.setDayProduceOrder(schedule.getDayProduceOrder());
			publishVo.setRecipeMaterialCode(schedule.getRecipeMaterialCode());
			publishVo.setMaterialName(schedule.getGlue());
			publishVo.setRecipeType(schedule.getRecipeType());
			publishVo.setRecipeVersionId(schedule.getRecipeVersionId());
			publishVo.setOrderNo(schedule.getOrderNo());
			// 调用发布接口前对数据进行校验
			AjaxResult validateResult = this.validatePublishData(publishVo);
			if (HttpStatus.ERROR == (Integer) validateResult.get(Constants.CODE)) {
				String resultMsg = (String) validateResult.get(AjaxResult.MSG_TAG);
				List<SchedulePublishLogVo> newErrorRecordlist = this.createErrorRecord(publishVo, resultMsg);
				for (SchedulePublishLogVo logVo: newErrorRecordlist) {
					if (GlueEngineConstants.SHIFT_CLASS_MID == logVo.getShiftClassId()) {
						schedule.setMidPublishStatus(ZltConstant.FAILURE_RELEASE);
					} else if (GlueEngineConstants.SHIFT_CLASS_NIGHT == logVo.getShiftClassId()) {
						schedule.setNightPublishStatus(ZltConstant.FAILURE_RELEASE);
					} else if (GlueEngineConstants.SHIFT_CLASS_DAY == logVo.getShiftClassId()) {
						schedule.setDayPublishStatus(ZltConstant.FAILURE_RELEASE);
					}
				}
				schedule.setReleaseStatus(ZltConstant.FAILURE_RELEASE);
				schedule.setBaseValue(schedule.getId());
				
				errorRecordList.addAll(newErrorRecordlist);
			} else {
				publishList.add(publishVo);
				readyPublishList.add(schedule);
			}
		}
		// 调用接口发送
		AjaxResult result = this.publishScheduleResult(publishList, errorRecordList, SCHEDULE_TYPE_GLUE);

		// 将发布状态全部更新到待发布记录中
		Map<Long, SchedulePublishVo> publishMap = publishList.stream()
				.collect(Collectors.toMap(SchedulePublishVo::getId, Function.identity()));
		Date currentDate = DateUtil.now();
		for (GlueScheduleResultVo schedule : readyPublishList) {
			SchedulePublishVo releasedVo = publishMap.get(schedule.getId());
			schedule.setReleaseStatus(releasedVo.getReleaseStatus());
			// 设置各班发布状态，1=发布成功；2=发布失败；4=超时
			schedule.setMidPublishStatus(
					Optional.ofNullable(releasedVo.getMidPublishStatus()).orElse(ZltConstant.TIMEOUT_FAILURE));
			schedule.setNightPublishStatus(
					Optional.ofNullable(releasedVo.getNightPublishStatus()).orElse(ZltConstant.TIMEOUT_FAILURE));
			schedule.setDayPublishStatus(
					Optional.ofNullable(releasedVo.getDayPublishStatus()).orElse(ZltConstant.TIMEOUT_FAILURE));
			if (ZltConstant.IS_RELEASE.equals(releasedVo.getReleaseStatus())) {
				schedule.setNewestPublishTime(currentDate);
				schedule.setPublishSuccessCount(schedule.getPublishSuccessCount() + 1);
			}
			schedule.setBaseValue(schedule.getId());
		}
		return result;
	}

	/**
	 * 向MES发布硫磺辅料排程记录
	 * 
	 * @param scheduleList
	 */
	@Override
	@Transactional
	public AjaxResult publishMaterialScheduleResult(List<MaterialScheduleResult> scheduleList) {
		// 加载机台名称
		machineNameMap = machineEngineService.mapMixMachineName(null);

		// 将硫磺辅料排程记录转换成胶料排程记录对象
		List<SchedulePublishVo> publishList = new ArrayList<>(scheduleList.size());
		List<SchedulePublishLogVo> errorRecordList = new ArrayList<>(scheduleList.size());
		List<MaterialScheduleResult> readyPublishList = new ArrayList<>(scheduleList.size());
		for (MaterialScheduleResult schedule : scheduleList) {
			SchedulePublishVo publishVo = new SchedulePublishVo();
			publishVo.setId(schedule.getId());
			publishVo.setMixArea(schedule.getMixArea());
			publishVo.setMachineCode(schedule.getMachineCode());
			publishVo.setScheduleDate(schedule.getScheduleDate());
			publishVo.setMidPlanQty(schedule.getMidPlanQty());
			publishVo.setMidProduceOrder(schedule.getMidProduceOrder());
			publishVo.setNightPlanQty(schedule.getNightPlanQty());
			publishVo.setNightProduceOrder(schedule.getNightProduceOrder());
			publishVo.setDayPlanQty(schedule.getDayPlanQty());
			publishVo.setDayProduceOrder(schedule.getDayProduceOrder());
			publishVo.setRecipeMaterialCode(schedule.getMaterialCode());
			publishVo.setMaterialName(schedule.getMaterialName());
			publishVo.setRecipeType(schedule.getRecipeType());
			publishVo.setRecipeVersionId(schedule.getRecipeVersionId());
			publishVo.setOrderNo(schedule.getOrderNo());
			// 调用发布接口前对数据进行校验
			AjaxResult validateResult = this.validatePublishData(publishVo);
			if (HttpStatus.ERROR == (Integer) validateResult.get(Constants.CODE)) {
				String resultMsg = (String) validateResult.get(AjaxResult.MSG_TAG);
				List<SchedulePublishLogVo> newErrorRecordlist = this.createErrorRecord(publishVo, resultMsg);
				for (SchedulePublishLogVo logVo: newErrorRecordlist) {
					if (GlueEngineConstants.SHIFT_CLASS_MID == logVo.getShiftClassId()) {
						schedule.setMidPublishStatus(ZltConstant.FAILURE_RELEASE);
					} else if (GlueEngineConstants.SHIFT_CLASS_NIGHT == logVo.getShiftClassId()) {
						schedule.setNightPublishStatus(ZltConstant.FAILURE_RELEASE);
					} else if (GlueEngineConstants.SHIFT_CLASS_DAY == logVo.getShiftClassId()) {
						schedule.setDayPublishStatus(ZltConstant.FAILURE_RELEASE);
					}
				}
				schedule.setReleaseStatus(ZltConstant.FAILURE_RELEASE);
				schedule.setBaseValue(schedule.getId());
				
				errorRecordList.addAll(newErrorRecordlist);
			} else {
				publishList.add(publishVo);
				readyPublishList.add(schedule);
			}
		}

		// 调用接口发送
		AjaxResult result = this.publishScheduleResult(publishList, errorRecordList, SCHEDULE_TYPE_MATERIAL);

		// 将发布状态全部更新到待发布记录中
		Map<Long, SchedulePublishVo> publishMap = publishList.stream()
				.collect(Collectors.toMap(SchedulePublishVo::getId, Function.identity()));
		Date currentDate = DateUtil.now();
		for (MaterialScheduleResult schedule : readyPublishList) {
			SchedulePublishVo releasedVo = publishMap.get(schedule.getId());
			schedule.setReleaseStatus(releasedVo.getReleaseStatus());
			// 设置各班发布状态，1=发布成功；2=发布失败；4=超时
			schedule.setMidPublishStatus(
					Optional.ofNullable(releasedVo.getMidPublishStatus()).orElse(ZltConstant.TIMEOUT_FAILURE));
			schedule.setNightPublishStatus(
					Optional.ofNullable(releasedVo.getNightPublishStatus()).orElse(ZltConstant.TIMEOUT_FAILURE));
			schedule.setDayPublishStatus(
					Optional.ofNullable(releasedVo.getDayPublishStatus()).orElse(ZltConstant.TIMEOUT_FAILURE));
			if (ZltConstant.IS_RELEASE.equals(releasedVo.getReleaseStatus())) {
				schedule.setNewestPublishTime(currentDate);
				schedule.setPublishSuccessCount(schedule.getPublishSuccessCount() + 1);
			}
			schedule.setBaseValue(schedule.getId());
		}
		return result;
	}

	/**
	 * 添加错误日志到发布错误列表中
	 * 
	 * @param errorRecordList 发布错误列表
	 * @param publishVo       待发布数据
	 * @param errorMsg        错误信息
	 */
	private List<SchedulePublishLogVo> createErrorRecord(SchedulePublishVo publishVo,
			String errorMsg) {
		List<SchedulePublishLogVo> tempList = new ArrayList<>(3);
		Double planQty = publishVo.getMidPlanQty();
		Integer planOrder = publishVo.getMidProduceOrder();
		if (planOrder != null && planQty != null && planQty > 0) {
			Integer shiftClass = GlueEngineConstants.SHIFT_CLASS_MID;
			SchedulePublishLogVo midErrorLog = this.createPublishErrorLog(publishVo, shiftClass, planOrder, planQty,
					errorMsg);
			tempList.add(midErrorLog);
		}
		planQty = publishVo.getNightPlanQty();
		planOrder = publishVo.getNightProduceOrder();
		if (planOrder != null && planQty != null && planQty > 0) {
			Integer shiftClass = GlueEngineConstants.SHIFT_CLASS_NIGHT;
			SchedulePublishLogVo midErrorLog = this.createPublishErrorLog(publishVo, shiftClass, planOrder, planQty,
					errorMsg);
			tempList.add(midErrorLog);
		}
		planQty = publishVo.getDayPlanQty();
		planOrder = publishVo.getDayProduceOrder();
		if (planOrder != null && planQty != null && planQty > 0) {
			Integer shiftClass = GlueEngineConstants.SHIFT_CLASS_DAY;
			SchedulePublishLogVo midErrorLog = this.createPublishErrorLog(publishVo, shiftClass, planOrder, planQty,
					errorMsg);
			tempList.add(midErrorLog);
		}
		if (CollectionUtils.isEmpty(tempList)) {
			// 如果计划量全部都为空，则单独产生一笔中班计划量为0的发布记录即可
			Integer shiftClass = GlueEngineConstants.SHIFT_CLASS_MID;
			SchedulePublishLogVo midErrorLog = this.createPublishErrorLog(publishVo, shiftClass, planOrder, planQty,
					errorMsg);
			tempList.add(midErrorLog);
		}
		return tempList;
	}

	/**
	 * 创建发布错误日志记录
	 * 
	 * @param schedule   待发布记录
	 * @param shiftClass 班别
	 * @param planOrder  生产顺序
	 * @param planQty    计划量
	 * @param errorMsg   错误信息
	 * @return
	 */
	private SchedulePublishLogVo createPublishErrorLog(SchedulePublishVo schedule, Integer shiftClass,
			Integer planOrder, Double planQty, String errorMsg) {
		Date planDate = schedule.getScheduleDate();
		String machineCode = schedule.getMachineCode();
		String materCode = schedule.getRecipeMaterialCode();
		String recipeType = schedule.getRecipeType();
		String recipeVersionId = schedule.getRecipeVersionId();
		String orderNo = schedule.getOrderNo();
		String mixArea = schedule.getMixArea();

		String newOrderNo = this.createOrderNo(orderNo, shiftClass);
		String planDateStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, planDate);
		Integer planNumInt = planQty != null? (int) Math.floor(planQty.doubleValue()): null;
		Integer recipeTypeInt = recipeType != null? new Integer(recipeType): null;
		Integer recipeVersion = recipeVersionId != null? new Integer(recipeVersionId): null;

		SchedulePublishLogVo publishLog = new SchedulePublishLogVo();
		publishLog.setScheduleDate(planDateStr);
		publishLog.setShiftClassId(shiftClass);
		publishLog.setMachineCode(machineCode);
		publishLog.setPlanQty(planNumInt);
		publishLog.setRecipeMaterialCode(materCode);
		publishLog.setRecipeType(recipeTypeInt);
		publishLog.setRecipeVersionId(recipeVersion);
		publishLog.setProduceOrder(planOrder);
		publishLog.setOrderNo(newOrderNo);
		publishLog.setMixArea(mixArea);
		publishLog.setPublishResult(WS_CODE_ERROR);
		publishLog.setRemark(errorMsg);
		publishLog.setBaseValue(null);
		return publishLog;
	}

	/**
	 * 发布数据校验
	 * 
	 * @param publishVo
	 * @return
	 */
	private AjaxResult validatePublishData(SchedulePublishVo publishVo) {
		if (StringUtils.isEmpty(publishVo.getRecipeMaterialCode())) {
			return AjaxResult.error(I18nUtil.getMessage("schedule.publish.error.isBlank.materialCode"));
		}
		if (StringUtils.isEmpty(publishVo.getMachineCode())) {
			return AjaxResult.error(I18nUtil.getMessage("schedule.publish.error.isBlank.machineCode"));
		}
		if (publishVo.getScheduleDate() == null) {
			return AjaxResult.error(I18nUtil.getMessage("schedule.publish.error.isBlank.scheduleDate"));
		}
		if (StringUtils.isEmpty(publishVo.getRecipeType())) {
			return AjaxResult.error(I18nUtil.getMessage("schedule.publish.error.isBlank.recipeType"));
		}
		if (StringUtils.isEmpty(publishVo.getRecipeVersionId())) {
			return AjaxResult.error(I18nUtil.getMessage("schedule.publish.error.isBlank.recipeVersionId"));
		}
		if (StringUtils.isEmpty(publishVo.getOrderNo())) {
			return AjaxResult.error(I18nUtil.getMessage("schedule.publish.error.isBlank.orderNo"));
		}
        if(publishVo.getMachineCode().contains(",")) {
			return AjaxResult.error(I18nUtil.getMessage("schedule.materialScheduleResult.machineCode.multiple"));
        }

		return AjaxResult.success();
	}

	/**
	 * 向MES发布排程记录
	 * 
	 * @param scheduleList    待发布排程列表
	 * @param errorRecordList 发布错误列表
	 * @param scheduleType    排程类型
	 */
	private AjaxResult publishScheduleResult(List<SchedulePublishVo> scheduleList,
			List<SchedulePublishLogVo> errorRecordList, String scheduleType) {
		if (CollectionUtil.isEmpty(scheduleList)) {
			return AjaxResult.error("没有可发布的排程记录");
		}

		// 查询已成功发布工单号
		SchedulePublishVo firstSchedule = CollectionUtil.firstElement(scheduleList);
		Date scheduleDate = firstSchedule.getScheduleDate();
		String mixArea = firstSchedule.getMixArea();
		// 查当日本密炼区成功的发布记录
		Map<String, SchedulePublishLogVo> publishOrderNoMap = schedulePublishEngineMapper
				.listLatestPublishLog(DateUtil.formatDate(scheduleDate), mixArea, scheduleType, WS_CODE_SUCCESS)
				.stream().collect(Collectors.toMap(SchedulePublishLogVo::getOrderNo, Function.identity()));

		boolean isAllSuccess = true; // 全局发布状态，只有出现网络连接超时会设置为false
		List<SchedulePublishLogVo> logList = new ArrayList<>();
		String errorMsg = null;
		// 循环调用接口
		for (SchedulePublishVo schedule : scheduleList) {
			Double midPlanQty = Optional.ofNullable(schedule.getMidPlanQty()).orElse(0D);
			Double nightPlanQty = Optional.ofNullable(schedule.getNightPlanQty()).orElse(0D);
			Double dayPlanQty = Optional.ofNullable(schedule.getDayPlanQty()).orElse(0D);
			Integer midPlanOrder = Optional.ofNullable(schedule.getMidProduceOrder()).orElse(0);
			Integer nightPlanOrder = Optional.ofNullable(schedule.getNightProduceOrder()).orElse(0);
			Integer dayPlanOrder = Optional.ofNullable(schedule.getDayProduceOrder()).orElse(0);

			Date planDate = schedule.getScheduleDate();
			String machineCode = schedule.getMachineCode();
			String materCode = schedule.getRecipeMaterialCode();
			String recipeType = schedule.getRecipeType();
			String recipeVersion = schedule.getRecipeVersionId();
			String orderNo = schedule.getOrderNo();
			String area = schedule.getMixArea();
			boolean isSuccess = isAllSuccess;

			// 调用接口发布，如果铨叙发布状态为false，后续的全部都不处理，直接置为失败
			// 发布中班
			publishMid: {
				if (!isAllSuccess) {
					break publishMid;
				}
				String mapKey = this.createOrderNo(orderNo, GlueEngineConstants.SHIFT_CLASS_MID);
				if (!checkIsChange(mapKey, publishOrderNoMap, midPlanQty, midPlanOrder, machineCode, recipeType,
						recipeVersion)) {
					// 如果与上一次发布比较无变化，则不需要触发发送接口，并直接标记未发送成功
					schedule.setMidPublishStatus(ZltConstant.IS_RELEASE);
					break publishMid;
				}

				AjaxResult result = this.addApsPlanInfo(planDate, orderNo, machineCode, materCode, recipeType,
						recipeVersion, midPlanQty, midPlanOrder, GlueEngineConstants.SHIFT_CLASS_MID);
				SchedulePublishLogVo logVo = (SchedulePublishLogVo) result.get(AjaxResult.DATA_TAG);
				if (HttpStatus.ERROR == (Integer) result.get(Constants.CODE)) {
					String resultMsg = (String) result.get(AjaxResult.MSG_TAG);
					errorMsg = this.buildErrorMesage(schedule, resultMsg);
					isSuccess = false;
					if (Optional.ofNullable((Boolean) result.get(TIMEOUT_TAG)).orElse(false)) { // 如果有出现连接超时情况，后续的均不再执行
						isAllSuccess = false;
					}
					logVo.setPublishResult(WS_CODE_ERROR);
					logVo.setRemark(resultMsg);
					schedule.setMidPublishStatus(ZltConstant.FAILURE_RELEASE);
				} else {
					logVo.setPublishResult(WS_CODE_SUCCESS);
					schedule.setMidPublishStatus(ZltConstant.IS_RELEASE);
				}
				logVo.setMixArea(area);
				logList.add(logVo);
			}
			// 发布夜班
			publishNight: {
				if (!isAllSuccess) {
					break publishNight;
				}
				String mapKey = this.createOrderNo(orderNo, GlueEngineConstants.SHIFT_CLASS_NIGHT);
				if (!checkIsChange(mapKey, publishOrderNoMap, nightPlanQty, nightPlanOrder, machineCode, recipeType,
						recipeVersion)) {
					schedule.setNightPublishStatus(ZltConstant.IS_RELEASE);
					break publishNight;
				}

				AjaxResult result = this.addApsPlanInfo(planDate, orderNo, machineCode, materCode, recipeType,
						recipeVersion, nightPlanQty, nightPlanOrder, GlueEngineConstants.SHIFT_CLASS_NIGHT);
				SchedulePublishLogVo logVo = (SchedulePublishLogVo) result.get(AjaxResult.DATA_TAG);
				if (HttpStatus.ERROR == (Integer) result.get(Constants.CODE)) {
					String resultMsg = (String) result.get(AjaxResult.MSG_TAG);
					isSuccess = false;
					if (Optional.ofNullable((Boolean) result.get(TIMEOUT_TAG)).orElse(false)) {
						isAllSuccess = false;
					}
					logVo.setPublishResult(WS_CODE_ERROR);
					logVo.setRemark(resultMsg);
					schedule.setNightPublishStatus(ZltConstant.FAILURE_RELEASE);
				} else {
					logVo.setPublishResult(WS_CODE_SUCCESS);
					schedule.setNightPublishStatus(ZltConstant.IS_RELEASE);
				}
				logVo.setMixArea(area);
				logList.add(logVo);
			}
			// 白班发布
			publishDay: {
				if (!isAllSuccess) {
					break publishDay;
				}
				String mapKey = this.createOrderNo(orderNo, GlueEngineConstants.SHIFT_CLASS_DAY);
				if (!checkIsChange(mapKey, publishOrderNoMap, dayPlanQty, dayPlanOrder, machineCode, recipeType,
						recipeVersion)) {
					schedule.setDayPublishStatus(ZltConstant.IS_RELEASE);
					break publishDay;
				}

				AjaxResult result = this.addApsPlanInfo(planDate, orderNo, machineCode, materCode, recipeType,
						recipeVersion, dayPlanQty, dayPlanOrder, GlueEngineConstants.SHIFT_CLASS_DAY);
				SchedulePublishLogVo logVo = (SchedulePublishLogVo) result.get(AjaxResult.DATA_TAG);
				if (HttpStatus.ERROR == (Integer) result.get(Constants.CODE)) {
					String resultMsg = (String) result.get(AjaxResult.MSG_TAG);
					errorMsg = this.buildErrorMesage(schedule, resultMsg);
					isSuccess = false;
					if (Optional.ofNullable((Boolean) result.get(TIMEOUT_TAG)).orElse(false)) {
						isAllSuccess = false;
					}
					logVo.setPublishResult(WS_CODE_ERROR);
					logVo.setRemark(resultMsg);
					schedule.setDayPublishStatus(ZltConstant.FAILURE_RELEASE);
				} else {
					logVo.setPublishResult(WS_CODE_SUCCESS);
					schedule.setDayPublishStatus(ZltConstant.IS_RELEASE);
				}
				logVo.setMixArea(area);
				logList.add(logVo);
			}

			// 发布状态
			String releaseStatus;
			if (isSuccess) {
				releaseStatus = ZltConstant.IS_RELEASE;
			} else { // 任意一个班发布失败都将该记录标成发布失败
				releaseStatus = ZltConstant.FAILURE_RELEASE;
			}
			schedule.setReleaseStatus(releaseStatus); // 更新状态
		}
		if (CollectionUtils.isNotEmpty(scheduleList)) {
			Long recordId = schedulePublishEngineMapper.getPublishRecordId();
			SchedulePublishRecordVo record = new SchedulePublishRecordVo();
			record.setScheduleDate(CollectionUtil.firstElement(scheduleList).getScheduleDate());
			record.setScheduleType(scheduleType);
			record.setBaseValue(null);
			record.setId(recordId);
			schedulePublishEngineMapper.savePublishRecord(record);
			if (CollectionUtils.isNotEmpty(errorRecordList)) {
				logList.addAll(errorRecordList);
			}
			logList.forEach(r -> r.setRecordId(recordId));
			if (CollectionUtils.isNotEmpty(logList)) {
				schedulePublishEngineMapper.savePublishLog(logList);
			}
		}

		if (StringUtils.isEmpty(errorMsg) && CollectionUtil.isEmpty(errorRecordList)) {
			return AjaxResult.success();
		} else {
			// 提示信息调整
			return AjaxResult.error(I18nUtil.getMessage("schedule.publish.error.msg"));
		}
	}

	/**
	 * 检查发布记录对比上一次发布是否有更改过
	 * 
	 * @param orderNo           工单号，带班次后缀
	 * @param publishOrderNoMap 发布记录列表
	 * @param newPlanQty        新计划量
	 * @param produceOrder      排产顺序
	 * @param machineCode       新机台号
	 * @param recipeType        配方类型
	 * @param recipeVersion     配方版本
	 * @return
	 */
	private boolean checkIsChange(String orderNo, Map<String, SchedulePublishLogVo> publishOrderNoMap,
			Double newPlanQty, Integer produceOrder, String machineCode, String recipeType, String recipeVersion) {
		SchedulePublishLogVo latestPublish = publishOrderNoMap.get(orderNo);
		if (newPlanQty.compareTo(0D) == 0 && latestPublish == null) { // 计划量为0，且之前没有发布过，则该笔记录无需发布
			return false;
		}
		if (latestPublish != null) { // 之前已经发布过，
			Integer planQty = Optional.ofNullable(latestPublish.getPlanQty()).orElse(0);
			return !Objects.equals(machineCode, latestPublish.getMachineCode())
					|| !Objects.equals(recipeType, String.valueOf(latestPublish.getRecipeType()))
					|| !Objects.equals(recipeVersion, String.valueOf(latestPublish.getRecipeVersionId()))
					|| !Objects.equals(produceOrder, latestPublish.getProduceOrder())
					|| planQty.compareTo((int) Math.floor(newPlanQty.doubleValue())) != 0;
		}
		return true;
	}

	/**
	 * 调用接口，并新增排程
	 * 
	 * @param conn           接口连接
	 * @param scheduleResult 待发布排程记录
	 * @param planNum        计划量
	 * @param planOrder      排产顺序
	 * @param shiftID        排产班别
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	private AjaxResult addApsPlanInfo(Date planDate, String orderNo, String machineCode, String materCode,
			String recipeType, String recipeVersionId, Double planQty, Integer planOrder, Integer shiftClass) {
		// 由于接口要求，同一个胶料同一天不同班的工单号要唯一，因此需要多拼接一个班别在末尾作为识别
		String newOrderNo = this.createOrderNo(orderNo, shiftClass);
		String planDateStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, planDate);
		Integer planNumInt = (int) Math.floor(planQty.doubleValue());
		Integer recipeTypeInt = new Integer(recipeType);
		Integer recipeVersion = new Integer(recipeVersionId);
		// 将参数设定到模板中
		String soap = StringUtils.format(WS_PARAMS_TEMPLATE, planDateStr, shiftClass, machineCode, planNumInt,
				materCode, recipeTypeInt, recipeVersion, planOrder, newOrderNo);

		// 构建发布日志记录
		SchedulePublishLogVo publishLog = new SchedulePublishLogVo();
		publishLog.setScheduleDate(planDateStr);
		publishLog.setShiftClassId(shiftClass);
		publishLog.setMachineCode(machineCode);
		publishLog.setPlanQty(planNumInt);
		publishLog.setRecipeMaterialCode(materCode);
		publishLog.setRecipeType(recipeTypeInt);
		publishLog.setRecipeVersionId(recipeVersion);
		publishLog.setProduceOrder(planOrder);
		publishLog.setOrderNo(newOrderNo);
		publishLog.setBaseValue(null);

		OutputStream os = null;
		InputStream is = null;
		HttpURLConnection conn = null;
		AjaxResult result = AjaxResult.success();
		try {
			// 创建与接口的连接
			URL wsUrl = new URL(pubishInterfaceUrl);
			conn = (HttpURLConnection) wsUrl.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			conn.setConnectTimeout(connectTimeout);
			conn.setReadTimeout(connectTimeout);
			// 将参数写入接口
			os = conn.getOutputStream();
			os.write(soap.toString().getBytes());

			// 抓取返回值
			is = conn.getInputStream();
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

			// 将反馈的文档转换为字符串
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty("encoding", "UTF-8");
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			transformer.transform(new DOMSource(doc), new StreamResult(outputStream));
			String docStr = outputStream.toString("UTF-8");
			publishLog.setInvokeResult(docStr);

			// 解析返回值
			result = this.parseResult(doc);
		} catch (SocketTimeoutException e) {
			log.error(e.getMessage(), e);
			if (publishLog.getInvokeResult() == null) {
				publishLog.setInvokeResult(e.getMessage());
			}
			result = AjaxResult.error(I18nUtil.getMessage("schedule.publish.error.mes.timeOut"));
			result.put(TIMEOUT_TAG, Boolean.TRUE);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			if (publishLog.getInvokeResult() == null) {
				publishLog.setInvokeResult(e.getMessage());
			}
			result = AjaxResult.error(I18nUtil.getMessage("schedule.publish.error.mes.error"));
		} catch (TransformerException | SAXException | ParserConfigurationException e) {
			log.error(e.getMessage(), e);
			if (publishLog.getInvokeResult() == null) {
				publishLog.setInvokeResult(e.getMessage());
			}
			result = AjaxResult.error(I18nUtil.getMessage("schedule.publish.error.mes.result.parse"));
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (os != null) {
					os.close();
				}
				if (conn != null) {
					conn.disconnect();
				}
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		result.put(AjaxResult.DATA_TAG, publishLog);
		return result;
	}

	/**
	 * 创建发布用的工单号
	 * 
	 * @param orderNo    原工单号
	 * @param shiftClass 班别
	 * @return
	 */
	private String createOrderNo(String orderNo, Integer shiftClass) {
		return new StringBuffer(orderNo).append(ORDER_NO_SPLIT).append(shiftClass).toString();
	}

	/**
	 * 解析webservice接口调用后反馈的文档
	 * 
	 * @param doc 返回文档
	 * @return
	 */

	private AjaxResult parseResult(Document doc) {
		// 解析返回值
		String result = null;
		String message = null;
		Element rootElement = doc.getDocumentElement();
		NodeList nodeList = rootElement.getElementsByTagName(WS_RESULT_KEY);
		if (nodeList.getLength() == 0) {
			throw new RuntimeException(I18nUtil.getMessage("schedule.publish.error.mes.result.format"));
		}
		NodeList apiResultList = nodeList.item(0).getChildNodes();
		for (int i = 0, len = apiResultList.getLength(); i < len; i++) {
			Node item = apiResultList.item(i);
			if (item.getFirstChild() == null) {
				continue;
			}
			if (WS_RESULT.equals(item.getNodeName())) {
				result = item.getFirstChild().getNodeValue();
			} else if (WS_MESSAGE.equals(item.getNodeName())) {
				message = item.getFirstChild().getNodeValue();
			}
		}
		if (!WS_CODE_SUCCESS.equals(result)) {
			String errorMessage = I18nUtil.getMessage("schedule.publish.error.mes.result.fail");
			return AjaxResult.error(StringUtils.format(errorMessage, result, message));
		}
		return AjaxResult.success();
	}

	private String buildErrorMesageWithI18n(SchedulePublishVo publishVo, String errorCode) {
		String errorMsg = I18nUtil.getMessage(errorCode);
		return buildErrorMesage(publishVo, errorMsg);
	}

	private String buildErrorMesage(SchedulePublishVo publishVo, String errorMsg) {
		String materialName = publishVo.getMaterialName();
		String machineCode = publishVo.getMachineCode();
		String machineName = machineNameMap.get(machineCode);
		String publishErrorMsgTemplate = I18nUtil.getMessage("schedule.publish.error.template");
		return StringUtils.format(publishErrorMsgTemplate, machineName, materialName, errorMsg);
	}
}
