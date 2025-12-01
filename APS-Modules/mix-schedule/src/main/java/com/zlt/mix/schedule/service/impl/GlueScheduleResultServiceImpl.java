package com.zlt.mix.schedule.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.common.core.utils.*;
import com.zlt.mix.common.core.vo.ScheduleSummaryVo;
import com.zlt.mix.schedule.api.domain.dto.*;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import com.zlt.mix.schedule.api.domain.entity.ScheduleOperLog;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.mapper.SchedulePublishEngineMapper;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEngineService;
import com.zlt.mix.schedule.engine.vo.SchedulePublishLogVo;
import com.zlt.mix.schedule.mapper.GlueScheduleResultMapper;
import com.zlt.mix.schedule.service.GlueScheduleResultService;
import com.zlt.mix.schedule.service.GlueSpanReceiveService;
import com.zlt.mix.schedule.service.GlueSpanSendService;
import com.zlt.mix.schedule.service.ScheduleOperLogService;
import com.zlt.mix.setting.api.domain.entity.FormulaMachine;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 终炼/母炼日计划排程Service业务层处理
 *
 * @author chen
 * @date 2022-05-16
 */
@Service
public class GlueScheduleResultServiceImpl extends ServiceImpl<GlueScheduleResultMapper, GlueScheduleResult> implements GlueScheduleResultService {
    @Resource
    private GlueScheduleResultMapper glueScheduleResultMapper;
    @Resource
    private GlueScheduleEngineService glueScheduleEngineService;
    @Resource
    private ScheduleOperLogService scheduleOperLogService;
    @Resource
    private GlueSpanSendService glueSpanSendService;
    @Resource
    private GlueSpanReceiveService glueSpanReceiveService;
	@Autowired
	private SchedulePublishEngineMapper schedulePublishEngineMapper;

    @Value("${excelModelPath}")
    public String excelModelPath;

	/**
	 * webservice接口状态码：失败
	 */
	private static final String WS_CODE_ERROR = "0";
	/**
	 * 工单号拼接分隔符
	 */
	private static final String ORDER_NO_SPLIT = "_";
	/**
	 * 发布类型：终炼母炼排程发布
	 */
	private static final String SCHEDULE_TYPE_GLUE = "0";
	/**
	 * html换行符
	 */
	private static final String BR = "<br/>";

	/**
	 * 中班换班时间
	 */
	private static String MID_CLASS_TIME = "2345";
	/**
	 * 夜班换班时间
	 */
	private static String NIGHT_CLASS_TIME = "0745";
	/**
	 * 白班换班时间
	 */
	private static String DAY_CLASS_TIME = "1545";

    /**
     * 查询终炼/母炼日计划排程列表
     *
     * @param glueScheduleResult 终炼/母炼日计划排程
     * @return 终炼/母炼日计划排程
     */
    @Override
    public List<GlueScheduleResult> selectGlueScheduleResultList(GlueScheduleResult glueScheduleResult) {
    	Date nowDate = DateUtils.getNowDate();
    	String newTime = DateUtils.parseDateToStr("HH", nowDate) + DateUtils.parseDateToStr("mm", nowDate); // 当前时分
    	String timeOrder;
    	if (newTime.compareTo(DAY_CLASS_TIME) > 0 && newTime.compareTo(MID_CLASS_TIME) <= 0) { // 中班时间段，按中班生产顺序排序
    		timeOrder = "ifnull(mid_produce_order, 9999)";
    	} else if (newTime.compareTo(NIGHT_CLASS_TIME) > 0 && newTime.compareTo(DAY_CLASS_TIME) <= 0) { // 白班时间段
    		timeOrder = "ifnull(day_produce_order, 9999)";
    	} else { // 其余均为夜班时间段
    		timeOrder = "ifnull(night_produce_order, 9999)";
    	}
    	String orderStr = glueScheduleResult.getOrderStr();
    	if (StringUtil.isNotEmpty(orderStr)) {
    		timeOrder = orderStr + "," + timeOrder;
    	}
    	glueScheduleResult.setOrderStr(timeOrder);
        List<GlueScheduleResult> list = glueScheduleResultMapper.selectGlueScheduleResultList(glueScheduleResult);
        this.setFinishRate(list);
        this.setReleaseStatusTip(glueScheduleResult, list);
        return list;
    }
    
    /**
     * 设置各班发布状态提示信息
     * @param list
     */
	private void setReleaseStatusTip(GlueScheduleResult glueScheduleResult, List<GlueScheduleResult> list) {
		Date scheduleDate = glueScheduleResult.getScheduleDate();
		String mixArea = glueScheduleResult.getMixArea();
		// 查当日本密炼区失败的发布记录
		Map<String, String> errorMessageMap = schedulePublishEngineMapper
				.listLatestPublishLog(DateUtil.formatDate(scheduleDate), mixArea, SCHEDULE_TYPE_GLUE, WS_CODE_ERROR)
				.stream().filter(s -> s.getOrderNo() != null && s.getRemark() != null)
				.collect(Collectors.toMap(SchedulePublishLogVo::getOrderNo, SchedulePublishLogVo::getRemark));

		// 设置各班发布状态提示信息
		String midErrorMessage = I18nUtil.getMessage("schedule.publish.error.mid");
		String nightErrorMessage = I18nUtil.getMessage("schedule.publish.error.night");
		String dayErrorMessage = I18nUtil.getMessage("schedule.publish.error.day");
		String timeOutErrorMessage = I18nUtil.getMessage("schedule.publish.error.mes.timeOut");
		for (GlueScheduleResult result : list) {
			// 发布失败的，需要将发布失败的班别信息展示到tip中
			if (!ZltConstant.FAILURE_RELEASE.equals(result.getReleaseStatus())) {
				continue;
			}
			StringBuilder releaseStatusTip = new StringBuilder();
			this.appendReleaseMessage(releaseStatusTip, GlueEngineConstants.SHIFT_CLASS_MID, result.getOrderNo(),
					result.getMidPublishStatus(), errorMessageMap, midErrorMessage, timeOutErrorMessage);
			this.appendReleaseMessage(releaseStatusTip, GlueEngineConstants.SHIFT_CLASS_NIGHT, result.getOrderNo(),
					result.getNightPublishStatus(), errorMessageMap, nightErrorMessage, timeOutErrorMessage);
			this.appendReleaseMessage(releaseStatusTip, GlueEngineConstants.SHIFT_CLASS_DAY, result.getOrderNo(),
					result.getDayPublishStatus(), errorMessageMap, dayErrorMessage, timeOutErrorMessage);

			if (releaseStatusTip.length() > 0) {
				result.setReleaseStatusTip(releaseStatusTip.toString());
			}
		}
	}

	/**
	 * 检查发布班次，如果发布失败，将发布失败信息的错误信息添加到tip中
	 * 
	 * @param releaseStatusTip    发布异常信息
	 * @param shiftClass          发布班次
	 * @param orderNo             工单号
	 * @param publishStatus       班次发布状态
	 * @param errorMessageMap     发布错误信息列表
	 * @param classErrorMessage   班次错误信息
	 * @param timeOutErrorMessage 超时错误信息
	 */
	private void appendReleaseMessage(StringBuilder releaseStatusTip, Integer shiftClass, String orderNo,
			String publishStatus, Map<String, String> errorMessageMap, String classErrorMessage,
			String timeOutErrorMessage) {
		if (ZltConstant.FAILURE_RELEASE.equals(publishStatus)) { // MES校验失败的情况
			releaseStatusTip.append(classErrorMessage);
			// 发布工单号，工单号_班次号
			String publishOrderNo = new StringBuilder().append(orderNo).append(ORDER_NO_SPLIT).append(shiftClass)
					.toString();
			// 从发布记录取出错误信息
			String errorMessage = errorMessageMap.get(publishOrderNo);
			if (StringUtils.isNotEmpty(errorMessage)) {
				releaseStatusTip.append(":").append(errorMessage);
			}
			releaseStatusTip.append(BR);
		} else if (ZltConstant.TIMEOUT_FAILURE.equals(publishStatus)) { // 网络连接异常的情况
			releaseStatusTip.append(classErrorMessage).append(":").append(timeOutErrorMessage).append(BR);
		}
	}

    /**
     * 保存终炼/母炼日计划排程信息（id为空则新增，id不为空则修改）
     */
    @Override
    public List<GlueScheduleResult> saveGlueScheduleResult(GlueScheduleResult glueScheduleResult) {
        glueScheduleResult.setBaseValue(glueScheduleResult.getId());
        setTotalPlanQty(glueScheduleResult);
        List<GlueScheduleResult> list = new ArrayList<>();
        GlueScheduleResult params = new GlueScheduleResult();
        params.setScheduleDate(glueScheduleResult.getScheduleDate());
        List<GlueScheduleResult> oldResultList = glueScheduleResultMapper.selectGlueScheduleResultList(params);
        Map<Long, GlueScheduleResult> oldResultMap = oldResultList.stream().collect(Collectors.toMap(GlueScheduleResult::getId, Function.identity()));
        if (glueScheduleResult.getId() != null) {
            GlueScheduleResult oldScheduleResult = getById(glueScheduleResult.getId());
            oldScheduleResult.setIsChangeMasterbatch(glueScheduleResult.getIsChangeMasterbatch());
            boolean isChangeSchedule = !this.compareScheduleField(oldScheduleResult, glueScheduleResult);
            boolean isChangeRemark = !this.compareRemarkField(oldScheduleResult, glueScheduleResult);
            // 如果有修改，将发布状态改为待发布
            if (isChangeSchedule || isChangeRemark) {
                // 设置编辑的字段，用于引擎操作
                GlueScheduleResult scheduleResult = setEditField(oldScheduleResult, glueScheduleResult);
                String releaseStatus = oldScheduleResult.getReleaseStatus(); // 发布状态，默认为原发布状态
            	if (isChangeSchedule) { // 如果有修改排程属性，则需要重算发布状态与预计时间等；如果只修改了备注，则不需要调整
            		releaseStatus = scheduleResult.getPublishSuccessCount() == 0 ? ZltConstant.NO_RELEASE : ZltConstant.WAIT_RELEASING;
                    // 调用引擎接口重算预计时间
                    list = glueScheduleEngineService.recaculateExpectTime(scheduleResult);
                    saveScheduleOperLogList(list, oldResultMap, glueScheduleResult.getOperIp());
            	} else {
            		list.add(scheduleResult);
            	}
            	scheduleResult.setReleaseStatus(releaseStatus);
                /*// 排程调量操作日志，有修改计划量，才记录日志
                if (!(compare(oldScheduleResult.getMidPlanQty(), glueScheduleResult.getMidPlanQty()) && compare(oldScheduleResult.getNightPlanQty(), glueScheduleResult.getNightPlanQty())
                        && compare(oldScheduleResult.getDayPlanQty(), glueScheduleResult.getDayPlanQty()))) {
                    saveScheduleOperLog(scheduleResult, oldScheduleResult, ZltConstant.OPER_TYPE_CHANGE_PLAN);
                }
                // 排程调序操作日志，有修改顺序，才记录日志
                if (!(compare(oldScheduleResult.getMidProduceOrder(), glueScheduleResult.getMidProduceOrder()) && compare(oldScheduleResult.getNightProduceOrder(), glueScheduleResult.getNightProduceOrder())
                        && compare(oldScheduleResult.getDayProduceOrder(), glueScheduleResult.getDayProduceOrder()))) {
                    saveScheduleOperLog(scheduleResult, oldScheduleResult, ZltConstant.OPER_TYPE_SEQUENCING);
                }*/
                glueScheduleResultMapper.updateScheduleResult(scheduleResult);
            }
        } else {
            // 插单
            glueScheduleResult.setDataSource(ZltConstant.GLUE_SCHEDULE_SOURCE_ADD);
            glueScheduleResult.setIsAddNew(true); // 插单需要标记成新增
            // 调用引擎插单接口
            list = glueScheduleEngineService.insertOrder(glueScheduleResult);
            // 排程插单操作日志
//            saveScheduleOperLog(glueScheduleResult, null, ZltConstant.OPER_TYPE_INSERT_ORDER);
            saveScheduleOperLogList(list, oldResultMap, glueScheduleResult.getOperIp());
        }
        if (list.size() > 0) {
            List<Long> ids = list.stream().map(GlueScheduleResult::getId).collect(Collectors.toList());
            list = glueScheduleResultMapper.selectByIds(ids);
            setFinishRate(list);
        }
        return list;
    }

    /**
     * 批量删除终炼/母炼日计划排程
     *
     * @param ids 需要删除的终炼/母炼日计划排程ID
     * @param isChangeMasterbatch 是否联级修改母炼胶标识
     * @return 结果
     */
    @Override
    public int deleteGlueScheduleResultByIds(Long[] ids, Boolean isChangeMasterbatch) {
    	// 重算待删除排程的胶料
//    	glueScheduleEngineService.recaculateTotalSurplus(Arrays.asList(ids), true);
        glueScheduleEngineService.deleteSchedule(Arrays.asList(ids), isChangeMasterbatch);
        return glueScheduleResultMapper.deleteGlueScheduleResultByIds(ids);
    }


    /**
     * 校验终炼/母炼日计划排程唯一性
     */
    @Override
    public String checkGlueScheduleResultUnique(GlueScheduleResult glueScheduleResult) {
        if (glueScheduleResult == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<GlueScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GlueScheduleResult::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(GlueScheduleResult::getScheduleDate, glueScheduleResult.getScheduleDate());
        queryWrapper.eq(GlueScheduleResult::getGlue, glueScheduleResult.getGlue());
        queryWrapper.eq(GlueScheduleResult::getMachineCode, glueScheduleResult.getMachineCode());
        queryWrapper.eq(GlueScheduleResult::getRecipeType, glueScheduleResult.getRecipeType());
        queryWrapper.eq(GlueScheduleResult::getRecipeVersionId, glueScheduleResult.getRecipeVersionId());
        queryWrapper.eq(StringUtil.isNotBlank(glueScheduleResult.getRecipeStage()),
                GlueScheduleResult::getRecipeStage, glueScheduleResult.getRecipeStage());
        if (glueScheduleResult.getId() != null) {
            queryWrapper.ne(GlueScheduleResult::getId, glueScheduleResult.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<GlueScheduleResult> list = glueScheduleResultMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入终炼/母炼日计划排程数据
     *
     * @param list         要导入的数据集合
     * @param scheduleDate 排程日期
     * @param mixArea      密炼区
     * @param importLogId  导入日志id
     */
    @Override
    public AjaxResult importData(List<GlueScheduleResult> list, Date scheduleDate, String mixArea, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<GlueScheduleResult> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        Map<Integer, Integer> importRowMap = new HashMap<>();//通过校验后的数据与在原本的Excel中对应的行数

        try {
            // 校验是否有已发布状态的日计划
            GlueScheduleResult glueScheduleResultParams = new GlueScheduleResult();
            glueScheduleResultParams.setScheduleDate(scheduleDate);
            glueScheduleResultParams.setMixArea(mixArea);
            List<GlueScheduleResult> oldScheduleList = this.selectGlueScheduleResultList(glueScheduleResultParams);
            if (oldScheduleList.stream().anyMatch(schedule -> schedule.getPublishSuccessCount() != null && schedule.getPublishSuccessCount() > 0)) {
            	String message = I18nUtil.getMessage("ui.biz.alter.publishedNotImport");
                addImportErrorLog(importLogId, null, message, importErrorLogs);
            	return AjaxResult.error(message, importErrorLogs);
            }

            // 机台数据，用于转换机台名称为机台编号保存
            FormulaMachine param = new FormulaMachine();
            param.setMixArea(mixArea);
            List<FormulaMachine> machineList = glueScheduleResultMapper.getFormulaMachineList(param);
            if (CollectionUtils.isEmpty(machineList)) {
                String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
                addImportErrorLog(importLogId, null, message, importErrorLogs);
                return AjaxResult.error(message, importErrorLogs);
            }
            Map<String, String> machineMap = machineList.stream().filter(item -> StringUtil.isNotBlank(item.getMachineName())).collect(Collectors.toMap(item -> item.getGlue() + item.getMachineName(), FormulaMachine::getMachineCode));

            // 查询配方表配方类型和配方类型名称对应map，用于转换
            List<RecipeType> recipeTypeInfo = glueScheduleResultMapper.getRecipeTypeInfo(new RecipeType());
            Map<String, String> recipeTypeInfoMap = recipeTypeInfo.stream().collect(Collectors.toMap(RecipeType::getRecipeTypeName, RecipeType::getRecipeTypeCode, (s, s2) -> s));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                GlueScheduleResult glueScheduleResult = list.get(i);
                // 发布状态默认未发布
                if (StringUtil.isBlank(glueScheduleResult.getReleaseStatus())) {
                    glueScheduleResult.setReleaseStatus(ZltConstant.NO_RELEASE);
                }
                //excel中重复记录校验
                String glue = glueScheduleResult.getGlue();
                String recipeTypeName = glueScheduleResult.getRecipeTypeName();
                int errorNum = i + 3;

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, glueScheduleResult); //校验excel每个单元格长度、类型等

                // 机台名称 转为机台code
                String machineName = glueScheduleResult.getMachineName();
                String machineCode = machineMap.get(glue + machineName);
                if (machineCode == null) {
                    String message = String.format(I18nUtil.getMessage("schedule.glueScheduleResult.message.import.machineNotExist"), mixArea, machineName, glue);
                    addImportErrorLog(importLogId, errorNum, message, validated);
                }
                // 配方类型名称 转为配方类型code
                if (StringUtil.isNotBlank(recipeTypeName) && recipeTypeInfoMap.get(recipeTypeName) == null) {
                    String message = String.format(I18nUtil.getMessage("schedule.glueScheduleResult.message.import.recipeTypeNotExist"), recipeTypeName);
                    addImportErrorLog(importLogId, errorNum, message, validated);
                }

                if (CollectionUtils.isEmpty(validated) && glueScheduleResult.getId() == null) {
                    glueScheduleResult.setBaseValue(null);
                    glueScheduleResult.setScheduleDate(scheduleDate);
                    glueScheduleResult.setMixArea(mixArea);
                    glueScheduleResult.setMachineCode(machineCode);
                    glueScheduleResult.setRecipeType(recipeTypeInfoMap.get(recipeTypeName));
                    glueScheduleResult.setDataSource(ZltConstant.GLUE_SCHEDULE_SOURCE_IMPORT);
                    importRowMap.put(importList.size(), i + 3);
                    importList.add(glueScheduleResult);
                } else {
                    glueScheduleResult.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }
            if (CollectionUtils.isNotEmpty(importList)) {
                /*codeUniqueErrorLogs = glueScheduleResultMapper.listGlueScheduleResultNotUnique(importList, importLogId,
                        I18nUtil.getMessage("schedule.glueScheduleResult.database.unique"), SecurityUtils.getUsername());

                //转换对应的错误行数、标记对应的错误记录
                for (ImportErrorLog codeUniqueErrorLog : codeUniqueErrorLogs) {
                    Integer errorRow = codeUniqueErrorLog.getErrorRow();
                    importList.get(errorRow).setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    codeUniqueErrorLog.setErrorRow(importRowMap.get(errorRow));
                }

                importErrorLogs.addAll(codeUniqueErrorLogs);*/

                // 过滤掉未通过校验的记录
                importList = importList.stream().filter(item -> item.getId() == null || !item.getId().equals(-999L)).collect(Collectors.toList());
            }

            // 参数 importList,id为-999的记录则在数据库内已存在
            log.debug("=======================终炼母联日计划导入集合信息start=========================");
            log.debug(importList.toString());
            if (CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();  //成功记录数
                // 调用引擎导入接口
//                glueScheduleResultMapper.mergeSql(importList);
            	List<ImportErrorLog> engineImportLogList = glueScheduleEngineService.importSchedule(importList);
            	successNum -= engineImportLogList.size(); // 扣减掉引擎校验失败的记录
            	engineImportLogList.stream().forEach(log -> {
            		log.setImportLogId(importLogId);
            		log.setErrorRow(importRowMap.get(log.getErrorRow()));
            	});
            	if (CollectionUtils.isNotEmpty(engineImportLogList)) {
            		importErrorLogs.addAll(engineImportLogList);
            	}
            }
            log.debug("=======================终炼母联日计划导入集合信息end=========================");
        } catch (Exception e) {
            log.error("导入出错", e);
            // 执行sql失败，插入导入失败记录
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        failureNum = list.size() - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 批量转机台(已废弃)
     */
    @Override
    public AjaxResult batchChangeMachine(String machineCode, Long[] ids) {
        // 根据id查询对应记录，校验是否唯一
        Collection<GlueScheduleResult> list = this.listByIds(Arrays.stream(ids).collect(Collectors.toList()));
        for (GlueScheduleResult glueScheduleResult : list) {
            //如果机台状态修改了
            if (!MixCommonUtil.compare(machineCode, glueScheduleResult.getMachineCode())) {
                //发布过改为待发布，否则改为已发布
                glueScheduleResult.setReleaseStatus(glueScheduleResult.getPublishSuccessCount() == 0 ? ZltConstant.NO_RELEASE : ZltConstant.WAIT_RELEASING);
            }
            glueScheduleResult.setMachineCode(machineCode);
        }
        List<GlueScheduleResult> notUniqueList = glueScheduleResultMapper.selectListUnique(list);
        if (CollectionUtils.isNotEmpty(notUniqueList)) {
            StringBuilder message = new StringBuilder();
            String format = I18nUtil.getMessage("schedule.glueScheduleResult.message.already.exists");
            for (GlueScheduleResult notUnique : notUniqueList) {
                String glue = notUnique.getGlue();
                message.append(String.format(format, glue)).append("<br>");
            }
            throw new RuntimeException(message.substring(0, message.lastIndexOf("<br>")));
        }
        // 调用引擎转机台接口
        glueScheduleEngineService.changeMachine(new ArrayList<>(list));
//        this.updateBatchById(list);
        return AjaxResult.success(list);
    }

    /**
     * 转机台
     *
     * @param glueScheduleResult 要转的机台，配方信息
     */
    @Override
    public AjaxResult changeMachine(GlueScheduleResult glueScheduleResult) {
        // 根据id查询对应记录，校验是否唯一
        Collection<GlueScheduleResult> list = this.listByIds(Collections.singletonList(glueScheduleResult.getId()));
        boolean isChange = false;
        for (GlueScheduleResult oldGlueScheduleResult : list) {
            //如果机台修改了
            isChange = !MixCommonUtil.compare(glueScheduleResult.getMachineCode(), oldGlueScheduleResult.getMachineCode());
            if (isChange) {
                //发布过改为待发布，否则改为已发布
                oldGlueScheduleResult.setReleaseStatus(oldGlueScheduleResult.getPublishSuccessCount() == 0 ? ZltConstant.NO_RELEASE : ZltConstant.WAIT_RELEASING);
            }
            oldGlueScheduleResult.setRecipeType(glueScheduleResult.getRecipeType());
            oldGlueScheduleResult.setRecipeTypeName(glueScheduleResult.getRecipeTypeName());
            oldGlueScheduleResult.setRecipeVersionId(glueScheduleResult.getRecipeVersionId());
            oldGlueScheduleResult.setRecipeStage(glueScheduleResult.getRecipeStage());
            oldGlueScheduleResult.setMachineCode(glueScheduleResult.getMachineCode());
            oldGlueScheduleResult.setIsChangeMasterbatch(glueScheduleResult.getIsChangeMasterbatch());
            oldGlueScheduleResult.setMidProduceOrder(glueScheduleResult.getMidProduceOrder());
            oldGlueScheduleResult.setNightProduceOrder(glueScheduleResult.getNightProduceOrder());
            oldGlueScheduleResult.setDayProduceOrder(glueScheduleResult.getDayProduceOrder());
        }
//        List<GlueScheduleResult> notUniqueList = glueScheduleResultMapper.selectListUnique(list);
//        if (CollectionUtils.isNotEmpty(notUniqueList)) {
//            StringBuilder message = new StringBuilder();
//            String msg = I18nUtil.getMessage("schedule.glueScheduleResult.database.unique");
//            for (GlueScheduleResult ignored : notUniqueList) {
//                message.append(msg).append("<br>");
//            }
//            throw new RuntimeException(message.substring(0, message.lastIndexOf("<br>")));
//        }
        GlueScheduleResult params = new GlueScheduleResult();
        params.setScheduleDate(glueScheduleResult.getScheduleDate());
        List<GlueScheduleResult> oldResultList = glueScheduleResultMapper.selectGlueScheduleResultList(params);
        Map<Long, GlueScheduleResult> oldResultMap = oldResultList.stream().collect(Collectors.toMap(GlueScheduleResult::getId, Function.identity()));
        // 调用引擎转机台接口
        List<GlueScheduleResult> newResultList = glueScheduleEngineService.changeMachine(new ArrayList<>(list));
        // 排程转机台操作日志
        if (isChange) {
//            saveScheduleOperLog(this.getById(glueScheduleResult.getId()), oldScheduleResult, ZltConstant.OPER_TYPE_CHANGE_MACHINE);
            saveScheduleOperLogList(newResultList, oldResultMap, glueScheduleResult.getOperIp());
        }
//        this.updateBatchById(list);
        return AjaxResult.success(newResultList.size() > 0 ? glueScheduleResultMapper.selectByIds(newResultList.stream().map(GlueScheduleResult::getId).collect(Collectors.toList())) : newResultList);
    }

    /**
     * 发布终炼母炼日计划
     */
    @Override
    public AjaxResult publish(GlueScheduleResult glueScheduleResult) {
        //对应的日计划是否存在机台为空、多个机台的数据，不允许发布
        AjaxResult ajaxResult = AjaxResult.error(I18nUtil.getMessage("ui.frame.alter.mustChooseOneRecord"));
        List<Long> ids = Arrays.asList(Convert.toLongArray(glueScheduleResult.getIds()));
        if(ids.size()>0){
            List<GlueScheduleResult> glueScheduleResultList = glueScheduleResultMapper.selectBatchIds(ids);
            for (GlueScheduleResult scheduleResult : glueScheduleResultList) {
                if(StringUtil.isEmpty(scheduleResult.getMachineCode())){
                    throw new RuntimeException(I18nUtil.getMessage("schedule.glueScheduleResult.machineCode.empty"));
                }
            }
            ajaxResult = glueScheduleEngineService.publishToMes(glueScheduleResult, ids);
        }
    	return ajaxResult;
    }

    /**
     * 更新下发状态
     * 
     * @param resultIdList 待下发的排程ID列表
     */
    public AjaxResult updateRelaseStatus(Long[] resultIdList, String relaseStatus) {
        return glueScheduleEngineService.updateRelaseStatus(Arrays.asList(resultIdList), relaseStatus);
    }

    /**
     * 比较排产属性值是否有更改
     *
     * @param oldScheduleResult  数据库内数据
     * @param glueScheduleResult 前端接收数据
     * @return 是否相同
     */
    private boolean compareScheduleField(GlueScheduleResult oldScheduleResult, GlueScheduleResult glueScheduleResult) {
        //以下是编辑页面开发修改的属性值
        boolean flag = MixCommonUtil.compare(oldScheduleResult.getMidPlanQty(), glueScheduleResult.getMidPlanQty());
        flag = flag && MixCommonUtil.compare(oldScheduleResult.getDayPlanQty(), glueScheduleResult.getDayPlanQty());
        flag = flag && MixCommonUtil.compare(oldScheduleResult.getNightPlanQty(), glueScheduleResult.getNightPlanQty());
        flag = flag && this.compare(oldScheduleResult.getMidProduceOrder(), glueScheduleResult.getMidProduceOrder());
        flag = flag && this.compare(oldScheduleResult.getDayProduceOrder(), glueScheduleResult.getDayProduceOrder());
        flag = flag && this.compare(oldScheduleResult.getNightProduceOrder(), glueScheduleResult.getNightProduceOrder());
        return flag;
    }

    /**
     * 比较备注字段是否有更改
     *
     * @param oldScheduleResult  数据库内数据
     * @param glueScheduleResult 前端接收数据
     * @return 是否相同
     */
    private boolean compareRemarkField(GlueScheduleResult oldScheduleResult, GlueScheduleResult glueScheduleResult) {
        //以下是编辑页面开发修改的属性值
        boolean flag = MixCommonUtil.compare(oldScheduleResult.getRemark(), glueScheduleResult.getRemark());
        flag = flag && MixCommonUtil.compare(oldScheduleResult.getMidRemark(), glueScheduleResult.getMidRemark());
        flag = flag && MixCommonUtil.compare(oldScheduleResult.getNightRemark(), glueScheduleResult.getNightRemark());
        flag = flag && MixCommonUtil.compare(oldScheduleResult.getDayRemark(), glueScheduleResult.getDayRemark());
        return flag;
    }

    /**
     * 根据参数查询机台信息
     *
     * @param param 参数
     */
    @Override
    public List<MixMachine> getMachineInfo(MixMachine param) {
        return glueScheduleResultMapper.getMachineInfo(param);
    }

    /**
     * 根据模板文件导出到Excel
     *
     * @param dto 参数
     * @return Excel字节数组
     */
    @Override
    public byte[] exportData(GlueScheduleResultExportDictDto dto) {
        List<GlueScheduleResult> list = this.selectGlueScheduleResultList(dto);
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + I18nUtil.getMessage("schedule.glueScheduleResult.modelName") + ".xlsx");
        Workbook webBook = ExcelUtils.readExcel(in);
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, String> recipeStageDictMap = dto.getRecipeStageDictMap();
            Map<String, String> mixAreaDictMap = dto.getMixAreaDictMap();
            Map<String, String> releaseStatusDictMap = dto.getReleaseStatusDictMap();
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            CellStyle percentCellStyle = ExcelUtils.createCellStyle(webBook);//创建单元格百分比格式
            percentCellStyle.setDataFormat(webBook.createDataFormat().getFormat("0.0%"));
            for (int i = 0; i < list.size(); i++) {
                GlueScheduleResult glueScheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
                int cellNum = 0;
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getMachineName() == null ? "" : glueScheduleResult.getMachineName());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getReleaseStatus() == null ? "" : releaseStatusDictMap.getOrDefault(glueScheduleResult.getReleaseStatus(), ""));
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getGlue() == null ? "" : glueScheduleResult.getGlue());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getRecipeTypeName() == null ? "" : glueScheduleResult.getRecipeTypeName());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getRecipeVersionId() == null ? "" : glueScheduleResult.getRecipeVersionId());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getRecipeStage() == null ? "" : recipeStageDictMap.getOrDefault(glueScheduleResult.getRecipeStage(), ""));
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getStockQty() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getStockQty());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getSafeStockQty() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getSafeStockQty());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getFormulaWeight() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getFormulaWeight());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getFormulaTime() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getFormulaTime());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getTotalPlanQty() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getTotalPlanQty());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getTotalSurplus() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getTotalSurplus());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getTotalFinish() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getTotalFinish());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getMidProduceOrder() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getMidProduceOrder());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getMidPlanQty() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getMidPlanQty());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getMidFinishQty() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getMidFinishQty());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getMidFinishRate() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getMidFinishRate());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getMidExpectStartTime() == null ? "" : DateFormatUtils.format(glueScheduleResult.getMidExpectStartTime(), "yyyy-MM-dd HH:mm:ss"));
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getMidExpectFinishTime() == null ? "" : DateFormatUtils.format(glueScheduleResult.getMidExpectFinishTime(), "yyyy-MM-dd HH:mm:ss"));
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getMidRemark() == null ? "" : glueScheduleResult.getMidRemark());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getNightProduceOrder() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getNightProduceOrder());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getNightPlanQty() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getNightPlanQty());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getNightFinishQty() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getNightFinishQty());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getNightFinishRate() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getNightFinishRate());
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getNightExpectStartTime() == null ? "" : DateFormatUtils.format(glueScheduleResult.getNightExpectStartTime(), "yyyy-MM-dd HH:mm:ss"));
                row.createCell(cellNum++).setCellValue(glueScheduleResult.getNightExpectFinishTime() == null ? "" : DateFormatUtils.format(glueScheduleResult.getNightExpectFinishTime(), "yyyy-MM-dd HH:mm:ss"));
                row.createCell(cellNum).setCellValue(glueScheduleResult.getNightRemark() == null ? "" : glueScheduleResult.getNightRemark());
                // row.createCell(cellNum++).setCellValue(glueScheduleResult.getDayProduceOrder() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getDayProduceOrder());
                // row.createCell(cellNum++).setCellValue(glueScheduleResult.getDayPlanQty() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getDayPlanQty());
                // row.createCell(cellNum++).setCellValue(glueScheduleResult.getDayFinishQty() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getDayFinishQty());
                // row.createCell(cellNum++).setCellValue(glueScheduleResult.getDayFinishRate() == null ? BigDecimal.ZERO.doubleValue() : glueScheduleResult.getDayFinishRate());
                // row.createCell(cellNum++).setCellValue(glueScheduleResult.getDayExpectStartTime() == null ? "" : DateFormatUtils.format(glueScheduleResult.getDayExpectStartTime(), "yyyy-MM-dd HH:mm:ss"));
                // row.createCell(cellNum++).setCellValue(glueScheduleResult.getDayExpectFinishTime() == null ? "" : DateFormatUtils.format(glueScheduleResult.getDayExpectFinishTime(), "yyyy-MM-dd HH:mm:ss"));
                // row.createCell(cellNum).setCellValue(glueScheduleResult.getDayRemark() == null ? "" : glueScheduleResult.getDayRemark());
                for (int j = 0; j <= cellNum; j++) {
                    // 完成率列设置百分比格式
                    if (j == 16 || j == 23 || j == 29) {
                        row.getCell(j).setCellStyle(percentCellStyle);
                        continue;
                    }
                    row.getCell(j).setCellStyle(cellStyle);
                }
            }
            Cell cell = sheet.getRow(0).getCell(0);
            Date scheduleDate = dto.getScheduleDate();
            String title = String.format(I18nUtil.getMessage("schedule.glueScheduleResult.tableTitle"),
                    scheduleDate == null ? "" : DateFormatUtils.format(scheduleDate, "yyyy-MM-dd"),
                    mixAreaDictMap.getOrDefault(dto.getMixArea(), ""));
            cell.setCellValue(title);
        }
        //写出字节流
        ByteArrayOutputStream out = null;
        byte[] data = null;
        try {
            out = new ByteArrayOutputStream();
            webBook.write(out);
            data = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
        }
        return data;
    }

    /**
     * 检测对应日期的数据是否存在
     *
     * @param glueScheduleResult 日期
     * @return 是否唯一的常量值
     */
    @Override
    public String checkScheduleDateAndMixAreaExist(GlueScheduleResult glueScheduleResult) {
        if (glueScheduleResult == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<GlueScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GlueScheduleResult::getScheduleDate, glueScheduleResult.getScheduleDate());
        queryWrapper.eq(GlueScheduleResult::getMixArea, glueScheduleResult.getMixArea());
        queryWrapper.eq(GlueScheduleResult::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);

        Long integer = glueScheduleResultMapper.selectCount(queryWrapper);
        if (integer != null && integer > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 更改配方信息
     *
     * @param glueScheduleResult id、配方阶段、配方版本号、配方类型
     * @return 结果
     */
    @Override
    public AjaxResult changeRecipe(GlueScheduleResult glueScheduleResult) {
        //比较配方阶段、配方版本号、配方类型是否修改了
        GlueScheduleResult oldScheduleResult = glueScheduleResultMapper.getById(glueScheduleResult.getId());
        oldScheduleResult.setIsChangeMasterbatch(glueScheduleResult.getIsChangeMasterbatch());
        boolean flag = MixCommonUtil.compare(oldScheduleResult.getRecipeStage(), glueScheduleResult.getRecipeStage());
        flag = flag && MixCommonUtil.compare(oldScheduleResult.getRecipeVersionId(), glueScheduleResult.getRecipeVersionId());
        flag = flag && MixCommonUtil.compare(oldScheduleResult.getRecipeType(), glueScheduleResult.getRecipeType());
        if (!flag) {
            glueScheduleResult.setReleaseStatus(oldScheduleResult.getPublishSuccessCount() == 0 ? ZltConstant.NO_RELEASE : ZltConstant.WAIT_RELEASING);
            oldScheduleResult.setReleaseStatus(glueScheduleResult.getReleaseStatus());
            oldScheduleResult.setRecipeType(glueScheduleResult.getRecipeType());
            oldScheduleResult.setRecipeVersionId(glueScheduleResult.getRecipeVersionId());
            oldScheduleResult.setRecipeStage(glueScheduleResult.getRecipeStage());
            oldScheduleResult.setFormulaWeight(glueScheduleResult.getFormulaWeight());
            oldScheduleResult.setFormulaTime(glueScheduleResult.getFormulaTime());
        }
        // 调用引擎接口重算预计时间
        GlueScheduleResult params = new GlueScheduleResult();
        params.setScheduleDate(glueScheduleResult.getScheduleDate());
        List<GlueScheduleResult> oldResultList = glueScheduleResultMapper.selectGlueScheduleResultList(params);
        Map<Long, GlueScheduleResult> oldResultMap = oldResultList.stream().collect(Collectors.toMap(GlueScheduleResult::getId, Function.identity()));
        List<GlueScheduleResult> list = glueScheduleEngineService.recaculateExpectTime(oldScheduleResult);
        saveScheduleOperLogList(list, oldResultMap, glueScheduleResult.getOperIp());
        glueScheduleResultMapper.changeRecipe(oldScheduleResult);
        return AjaxResult.success(list.size() > 0 ? glueScheduleResultMapper.selectByIds(list.stream().map(GlueScheduleResult::getId).collect(Collectors.toList())) : list);
    }

    /**
     * 根据id查询排程结果信息
     * @param id id
     * @return 查询到的记录
     */
    @Override
    public GlueScheduleResult getById(Long id) {
        GlueScheduleResult scheduleResult = glueScheduleResultMapper.getById(id);
        if (scheduleResult == null) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.noRecord"));
        }
        setFinishRate(Collections.singletonList(scheduleResult));
        return scheduleResult;
    }

    /**
     * 根据ids查询发布状态是否有不是【未发布】的记录
     * @param ids ids
     * @return 不是未发布的记录数
     */
    @Override
    public int isNoReleaseByIds(Long[] ids) {
        return glueScheduleResultMapper.isNoReleaseByIds(ids);
    }

    /**
     * 获取统计信息
     * @param glueScheduleResult 日期、密炼区、机台编号
     * @return 统计好的信息列表
     */
    @Override
    public List<GlueScheduleResultStatisticsDto> statistics(GlueScheduleResult glueScheduleResult) {
        List<GlueScheduleResultStatisticsDto> statistics = glueScheduleResultMapper.statistics(glueScheduleResult);
        //回显分厂没有提报
        if(CollectionUtils.isNotEmpty(statistics)){
            GlueScheduleResultStatisticsDto glueScheduleResultStatisticsDto = statistics.get(statistics.size() - 1);
            if(StringUtil.isEmpty(glueScheduleResultStatisticsDto.getMachineName())){
                glueScheduleResultStatisticsDto.setMachineName(I18nUtil.getMessage("schedule.glueScheduleResult.defaultMachineName"));
            }
        }
        return statistics;
    }

    /**
     * 根据条件查询终炼母炼日计划跨区发送列表
     *
     * @param entity 查询条件
     * @return 结果
     */
    @Override
    public List<GlueSpanSend> listGlueSpanSend(GlueSpanSend entity) {
        entity.setSource(ZltConstant.SOURCE_GLUE_SCHEDULE_RESULT);
        return glueSpanSendService.listGlueSpanSend(entity);
    }

    /**
     * 发送跨区请求
     *
     * @param dto 跨区请求集合
     * @return 结果
     */
    @Override
    public AjaxResult sendGlueSpan(GlueSpanSendDto dto) throws ParseException {
        List<GlueSpanSend> list = dto.getGlueSpanSendList();
        if (CollectionUtils.isNotEmpty(list)) {
            List<GlueSpanReceive> glueSpanReceiveList = new ArrayList<>();
            
            // 日期校验
            Date currentDate = DateUtils.getNowDate(DateUtils.YYYY_MM_DD);
            for (GlueSpanSend glueSpanSend : list) {
            	Date scheduleDate = glueSpanSend.getScheduleDate();
            	if (scheduleDate == null || scheduleDate.compareTo(currentDate) < 0) {
            		return AjaxResult.error("排程日期不可早于今天！");
            	}
            }
            for (GlueSpanSend glueSpanSend : list) {
                glueSpanSend.setBaseValue(null);
//                glueSpanSend.setScheduleDate(DateUtils.addDays(DateUtils.getNowDate("yyyy-MM-dd"), 1));
                glueSpanSend.setSendTime(new Date());
                glueSpanSend.setReceiveStatus(ZltConstant.RECEIVE_STATUS_NO);
                glueSpanSend.setSource(ZltConstant.SOURCE_GLUE_SCHEDULE_RESULT);
                glueSpanSend.setIsAuto(ZltConstant.IS_AUTO_NO);
                GlueSpanReceive glueSpanReceive = new GlueSpanReceive();
                BeanUtils.copyProperties(glueSpanSend, glueSpanReceive);
                glueSpanSendService.insertGlueSpanSend(glueSpanSend);
                glueSpanReceive.setSendId(glueSpanSend.getId());
                glueSpanReceiveList.add(glueSpanReceive);
            }
            glueSpanReceiveService.batchInsertGlueSpanReceive(glueSpanReceiveList);
        }
        return AjaxResult.success();
    }

    /**
     * 根据条件查询终炼母炼日计划跨区接收列表
     *
     * @param entity 查询条件
     * @return 结果
     */
    @Override
    public List<GlueSpanReceive> listGlueSpanReceive(GlueSpanReceive entity) {
        return glueSpanReceiveService.listGlueSpanReceive(entity);
    }

    /**
     * 接收跨区请求
     *
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @Override
    public AjaxResult receiveGlueSpanReceive(GlueSpanReceiveDto dto) {
        List<GlueSpanReceive> receiveList = dto.getGlueSpanReceiveList();
        Long[] ids = new Long[receiveList.size()];
        for (int i = 0; i < receiveList.size(); i++) {
            GlueSpanReceive glueSpanReceive = receiveList.get(i);
            glueSpanReceive.setBaseValue(glueSpanReceive.getId());
            glueSpanReceive.setReceiveStatus(ZltConstant.RECEIVE_STATUS_YES);
            glueSpanReceive.setReceiveTime(new Date());
            ids[i] = glueSpanReceive.getId();
        }
        if (glueSpanReceiveService.getAlreadyReceivedCountByIds(ids) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.receive.alreadyReceive"));
        }
        glueSpanReceiveService.mergeGlueSpanReceive(receiveList);
        glueSpanSendService.mergeGlueSpanSend(receiveList);
        return AjaxResult.success();
    }

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     *
     * @param glueScheduleResult 参数
     * @return 结果
     */
    @Override
    public GlueSpanReceiveQtyDto getSumQtyByMachineCode(GlueScheduleResult glueScheduleResult) {
        return glueScheduleResultMapper.getSumQtyByMachineCode(glueScheduleResult);
    }

    /**
     * 根据id删除跨区发送记录
     * @param ids id
     * @return 结果
     */
    @Override
    public AjaxResult deleteGlueSpanSend(Long[] ids) {
        // 校验是否已接收
        Integer alreadyReceivedCount = glueSpanSendService.getAlreadyReceivedCount(ids);
        if (alreadyReceivedCount > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.send.alreadyReceive"));
        }
        if (glueSpanReceiveService.getAlreadyReceivedCount(ids) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.send.alreadyReceive"));
        }
        glueSpanSendService.deleteByIds(ids);
        glueSpanReceiveService.deleteBySendIds(ids);
        return AjaxResult.success();
    }

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     *
     * @param ids 选中的id
     * @return 查询结果
     */
    @Override
    public List<GlueScheduleResult> selectSpanSendNeedFieldByIds(Long[] ids) {
        return glueScheduleResultMapper.selectSpanSendNeedFieldByIds(ids);
    }
    
	/**
	 * 终炼胶母炼胶日计划自动排程
	 * 
	 */
    @Override
    public void autoGlueSchedule(GlueScheduleResult glueScheduleResult) {
    	Date scheduleDate = glueScheduleResult.getScheduleDate();
    	String mixArea = glueScheduleResult.getMixArea();
    	glueScheduleEngineService.autoGlueSchedule(scheduleDate, mixArea); // 调用引擎自动排程接口
		// 记录自动排程日志
		List<GlueScheduleResult> newGlueScheduleResultList = glueScheduleResultMapper.selectGlueScheduleResultList(glueScheduleResult); // 查询已生成记录
		List<ScheduleOperLog> operLogs = new ArrayList<>();
		String operIp = glueScheduleResult.getOperIp(); // 操作IP
		for (GlueScheduleResult newGlueScheduleResult : newGlueScheduleResultList) {
            Long newResultId = newGlueScheduleResult.getId();
            this.addOperLogToList(ZltConstant.OPER_TYPE_AUTO_SCHEDULE, newResultId, newGlueScheduleResult, null, operLogs, operIp);
		}
        if (CollectionUtils.isNotEmpty(operLogs)) {
            scheduleOperLogService.batchInsertScheduleOperLogInfo(operLogs);
        }
    }

    /**
     * 计算总计划量
     * @param glueScheduleResult 中班、夜班、白班计划量总和计算
     */
    private void setTotalPlanQty(GlueScheduleResult glueScheduleResult) {
        Double midPlanQty = glueScheduleResult.getMidPlanQty() == null ? 0 : glueScheduleResult.getMidPlanQty();
        Double nightPlanQty = glueScheduleResult.getNightPlanQty() == null ? 0 : glueScheduleResult.getNightPlanQty();
        Double dayPlanQty = glueScheduleResult.getDayPlanQty() == null ? 0 : glueScheduleResult.getDayPlanQty();
        glueScheduleResult.setTotalPlanQty(BigDecimalUtil.add(midPlanQty, nightPlanQty, dayPlanQty));
    }

    /**
     * 批量保存排程操作日志
     * @param newGlueScheduleResultList 要保存的排程操作日志集合
     * @param oldResultMap 要保存排程日志当天的所有排程旧数据
     */
    private void saveScheduleOperLogList(List<GlueScheduleResult> newGlueScheduleResultList, Map<Long, GlueScheduleResult> oldResultMap, String operIp) {
        Map<String, GlueScheduleResult> scheduleMap = newGlueScheduleResultList.stream()
        	    .collect(Collectors.toMap(GlueScheduleResult::getOrderNo, Function.identity(), (s1, s2) -> s1));
        List<ScheduleOperLog> operLogs = new ArrayList<>();
        for (GlueScheduleResult glueScheduleResult : newGlueScheduleResultList) {
            Long newResultId = glueScheduleResult.getId();
            String operType = "";
            if (oldResultMap.containsKey(newResultId)) {
                GlueScheduleResult oldResult = oldResultMap.get(newResultId);
                // 排程调序操作日志，有修改计划，才记录日志
                if (!(MixCommonUtil.compare(oldResult.getMidPlanQty(), glueScheduleResult.getMidPlanQty()) && MixCommonUtil.compare(oldResult.getNightPlanQty(), glueScheduleResult.getNightPlanQty())
                        && MixCommonUtil.compare(oldResult.getDayPlanQty(), glueScheduleResult.getDayPlanQty()))) {
                    operType = ZltConstant.OPER_TYPE_CHANGE_PLAN;
                    addOperLogToList(operType, newResultId, glueScheduleResult, oldResult, operLogs, operIp);
                }
                // 排程调序操作日志，有修改顺序，才记录日志
                if (!(MixCommonUtil.compare(oldResult.getMidProduceOrder(), glueScheduleResult.getMidProduceOrder()) && MixCommonUtil.compare(oldResult.getNightProduceOrder(), glueScheduleResult.getNightProduceOrder())
                        && MixCommonUtil.compare(oldResult.getDayProduceOrder(), glueScheduleResult.getDayProduceOrder()))) {
                    operType = ZltConstant.OPER_TYPE_SEQUENCING;
                    addOperLogToList(operType, newResultId, glueScheduleResult, oldResult, operLogs, operIp);
                }
                // 排程调序操作日志，有修改机台，才记录日志
                if (!MixCommonUtil.compare(glueScheduleResult.getMachineCode(), oldResult.getMachineCode())){
                    operType = ZltConstant.OPER_TYPE_CHANGE_MACHINE;
                    addOperLogToList(operType, newResultId, glueScheduleResult, oldResult, operLogs, operIp);
                }
            } else {
            	// 先判断是不是因转机台生成的新记录
            	String sourceOrderNo = glueScheduleResult.getSourceOrderNo();
            	if (sourceOrderNo != null && scheduleMap.get(sourceOrderNo) != null) { // 转机台生成的，记录转机台日志
            		GlueScheduleResult oldResult = scheduleMap.get(sourceOrderNo);
                    operType = ZltConstant.OPER_TYPE_CHANGE_MACHINE;
                    addOperLogToList(operType, newResultId, glueScheduleResult, oldResult, operLogs, operIp);
                } else {
                    operType = ZltConstant.OPER_TYPE_INSERT_ORDER;
                    addOperLogToList(operType, newResultId, glueScheduleResult, null, operLogs, operIp);
            	}
            }
        }
        if (CollectionUtils.isNotEmpty(operLogs)) {
            scheduleOperLogService.batchInsertScheduleOperLogInfo(operLogs);
        }
    }

    /**
     * 构建要保存的排程操作日志，添加到要保存的集合中
     * @param operType 操作类型
     * @param newResultId 操作后的排程记录id
     * @param glueScheduleResult 操作后排程记录
     * @param oldResultMap 操作前排程数据map
     * @param operLogs 要保存的操作日志集合
     */
    private void addOperLogToList(String operType, Long newResultId, GlueScheduleResult glueScheduleResult, GlueScheduleResult oldGlueScheduleResult, List<ScheduleOperLog> operLogs, String operIp) {
        ScheduleOperLog scheduleOperLog = new ScheduleOperLog();
        scheduleOperLog.setScheduleType(ZltConstant.OPER_SCHEDULE_TYPE_GLUE);
        scheduleOperLog.setScheduleId(newResultId);
        scheduleOperLog.setOperType(operType);
        scheduleOperLog.setOperIp(operIp);
        scheduleOperLog.setScheduleDate(glueScheduleResult.getScheduleDate());
        scheduleOperLog.setMixArea(glueScheduleResult.getMixArea());
        scheduleOperLog.setMaterialCode(glueScheduleResult.getGlue());

        if (oldGlueScheduleResult != null) {
        	scheduleOperLog.setBeforeReleaseStatus(oldGlueScheduleResult.getReleaseStatus());
            scheduleOperLog.setBeforeMachineCode(oldGlueScheduleResult.getMachineCode());
            scheduleOperLog.setBeforeRecipeType(oldGlueScheduleResult.getRecipeType());
            scheduleOperLog.setBeforeRecipeVersionId(oldGlueScheduleResult.getRecipeVersionId());
            scheduleOperLog.setBeforeRecipeStage(oldGlueScheduleResult.getRecipeStage());
            Double midPlanQty = oldGlueScheduleResult.getMidPlanQty();
            scheduleOperLog.setBeforeMidPlan(ObjectUtils.isEmpty(midPlanQty) ? BigDecimal.ZERO.doubleValue() : midPlanQty);
            Integer midProduceOrder = oldGlueScheduleResult.getMidProduceOrder();
            scheduleOperLog.setBeforeMidOrder(ObjectUtils.isEmpty(midProduceOrder) ? BigDecimal.ZERO.intValue() : midProduceOrder);
            Double nightPlanQty = oldGlueScheduleResult.getNightPlanQty();
            scheduleOperLog.setBeforeNightPlan(ObjectUtils.isEmpty(nightPlanQty) ? BigDecimal.ZERO.doubleValue() : nightPlanQty);
            Integer nightProduceOrder = oldGlueScheduleResult.getNightProduceOrder();
            scheduleOperLog.setBeforeNightOrder(ObjectUtils.isEmpty(nightProduceOrder) ? BigDecimal.ZERO.intValue() : nightProduceOrder);
            Double dayPlanQty = oldGlueScheduleResult.getDayPlanQty();
            scheduleOperLog.setBeforeDayPlan(ObjectUtils.isEmpty(dayPlanQty) ? BigDecimal.ZERO.doubleValue() : dayPlanQty);
            Integer dayProduceOrder = oldGlueScheduleResult.getDayProduceOrder();
            scheduleOperLog.setBeforeDayOrder(ObjectUtils.isEmpty(dayProduceOrder) ? BigDecimal.ZERO.intValue() : dayProduceOrder);
        }

    	scheduleOperLog.setAfterReleaseStatus(glueScheduleResult.getReleaseStatus());
        scheduleOperLog.setAfterMachineCode(glueScheduleResult.getMachineCode());
        scheduleOperLog.setAfterRecipeType(glueScheduleResult.getRecipeType());
        scheduleOperLog.setAfterRecipeVersionId(glueScheduleResult.getRecipeVersionId());
        scheduleOperLog.setAfterRecipeStage(glueScheduleResult.getRecipeStage());
        Double midPlanQty = glueScheduleResult.getMidPlanQty();
        scheduleOperLog.setAfterMidPlan(ObjectUtils.isEmpty(midPlanQty) ? BigDecimal.ZERO.doubleValue() : midPlanQty);
        Integer midProduceOrder = glueScheduleResult.getMidProduceOrder();
        scheduleOperLog.setAfterMidOrder(ObjectUtils.isEmpty(midProduceOrder) ? BigDecimal.ZERO.intValue() : midProduceOrder);
        Double nightPlanQty = glueScheduleResult.getNightPlanQty();
        scheduleOperLog.setAfterNightPlan(ObjectUtils.isEmpty(nightPlanQty) ? BigDecimal.ZERO.doubleValue() : nightPlanQty);
        Integer nightProduceOrder = glueScheduleResult.getNightProduceOrder();
        scheduleOperLog.setAfterNightOrder(ObjectUtils.isEmpty(nightProduceOrder) ? BigDecimal.ZERO.intValue() : nightProduceOrder);
        Double dayPlanQty = glueScheduleResult.getDayPlanQty();
        scheduleOperLog.setAfterDayPlan(ObjectUtils.isEmpty(dayPlanQty) ? BigDecimal.ZERO.doubleValue() : dayPlanQty);
        Integer dayProduceOrder = glueScheduleResult.getDayProduceOrder();
        scheduleOperLog.setAfterDayOrder(ObjectUtils.isEmpty(dayProduceOrder) ? BigDecimal.ZERO.intValue() : dayProduceOrder);
        scheduleOperLog.setBaseValue(null);
        operLogs.add(scheduleOperLog);
    }

    /**
     * 保存排程操作日志
     * @param newGlueScheduleResult 操作后的排程操作日志
     * @param operType 操作类型
     */
    private void saveScheduleOperLog(GlueScheduleResult newGlueScheduleResult, GlueScheduleResult oldScheduleResult, String operType) {

        ScheduleOperLog scheduleOperLog = new ScheduleOperLog();
        scheduleOperLog.setScheduleType(ZltConstant.OPER_SCHEDULE_TYPE_GLUE);
        scheduleOperLog.setScheduleId(newGlueScheduleResult.getId());
        scheduleOperLog.setOperType(operType);
        scheduleOperLog.setScheduleDate(newGlueScheduleResult.getScheduleDate());
        scheduleOperLog.setMixArea(newGlueScheduleResult.getMixArea());
        scheduleOperLog.setMaterialCode(newGlueScheduleResult.getGlue());

        if (!ZltConstant.OPER_TYPE_INSERT_ORDER.equals(operType)) {
            GlueScheduleResult oldGlueScheduleResult = oldScheduleResult == null ? glueScheduleResultMapper.getById(newGlueScheduleResult.getId()) : oldScheduleResult;
            scheduleOperLog.setBeforeMachineCode(oldGlueScheduleResult.getMachineCode());
            scheduleOperLog.setBeforeRecipeType(oldGlueScheduleResult.getRecipeType());
            scheduleOperLog.setBeforeRecipeVersionId(oldGlueScheduleResult.getRecipeVersionId());
            scheduleOperLog.setBeforeRecipeStage(oldGlueScheduleResult.getRecipeStage());
            Double midPlanQty = oldGlueScheduleResult.getMidPlanQty();
            scheduleOperLog.setBeforeMidPlan(ObjectUtils.isEmpty(midPlanQty) ? BigDecimal.ZERO.doubleValue() : midPlanQty);
            Integer midProduceOrder = oldGlueScheduleResult.getMidProduceOrder();
            scheduleOperLog.setBeforeMidOrder(ObjectUtils.isEmpty(midProduceOrder) ? BigDecimal.ZERO.intValue() : midProduceOrder);
            Double nightPlanQty = oldGlueScheduleResult.getNightPlanQty();
            scheduleOperLog.setBeforeNightPlan(ObjectUtils.isEmpty(nightPlanQty) ? BigDecimal.ZERO.doubleValue() : nightPlanQty);
            Integer nightProduceOrder = oldGlueScheduleResult.getNightProduceOrder();
            scheduleOperLog.setBeforeNightOrder(ObjectUtils.isEmpty(nightProduceOrder) ? BigDecimal.ZERO.intValue() : nightProduceOrder);
            Double dayPlanQty = oldGlueScheduleResult.getDayPlanQty();
            scheduleOperLog.setBeforeDayPlan(ObjectUtils.isEmpty(dayPlanQty) ? BigDecimal.ZERO.doubleValue() : dayPlanQty);
            Integer dayProduceOrder = oldGlueScheduleResult.getDayProduceOrder();
            scheduleOperLog.setBeforeDayOrder(ObjectUtils.isEmpty(dayProduceOrder) ? BigDecimal.ZERO.intValue() : dayProduceOrder);
        }

        scheduleOperLog.setAfterMachineCode(newGlueScheduleResult.getMachineCode());
        scheduleOperLog.setAfterRecipeType(newGlueScheduleResult.getRecipeType());
        scheduleOperLog.setAfterRecipeVersionId(newGlueScheduleResult.getRecipeVersionId());
        scheduleOperLog.setAfterRecipeStage(newGlueScheduleResult.getRecipeStage());
        Double midPlanQty = newGlueScheduleResult.getMidPlanQty();
        scheduleOperLog.setAfterMidPlan(ObjectUtils.isEmpty(midPlanQty) ? BigDecimal.ZERO.doubleValue() : midPlanQty);
        Integer midProduceOrder = newGlueScheduleResult.getMidProduceOrder();
        scheduleOperLog.setAfterMidOrder(ObjectUtils.isEmpty(midProduceOrder) ? BigDecimal.ZERO.intValue() : midProduceOrder);
        Double nightPlanQty = newGlueScheduleResult.getNightPlanQty();
        scheduleOperLog.setAfterNightPlan(ObjectUtils.isEmpty(nightPlanQty) ? BigDecimal.ZERO.doubleValue() : nightPlanQty);
        Integer nightProduceOrder = newGlueScheduleResult.getNightProduceOrder();
        scheduleOperLog.setAfterNightOrder(ObjectUtils.isEmpty(nightProduceOrder) ? BigDecimal.ZERO.intValue() : nightProduceOrder);
        Double dayPlanQty = newGlueScheduleResult.getDayPlanQty();
        scheduleOperLog.setAfterDayPlan(ObjectUtils.isEmpty(dayPlanQty) ? BigDecimal.ZERO.doubleValue() : dayPlanQty);
        Integer dayProduceOrder = newGlueScheduleResult.getDayProduceOrder();
        scheduleOperLog.setAfterDayOrder(ObjectUtils.isEmpty(dayProduceOrder) ? BigDecimal.ZERO.intValue() : dayProduceOrder);
        scheduleOperLog.setBaseValue(null);

        scheduleOperLogService.saveScheduleOperLog(scheduleOperLog);
    }

    /**
     * 设置有涉及到编辑的字段赋值
     * @param oldScheduleResult 旧数据
     * @param glueScheduleResult 修改后新数据
     * @return 设值完成后对象
     */
    private GlueScheduleResult setEditField(GlueScheduleResult oldScheduleResult, GlueScheduleResult glueScheduleResult) {
        GlueScheduleResult result = new GlueScheduleResult();
        BeanUtils.copyProperties(oldScheduleResult, result);
        result.setBaseValue(result.getId());
        result.setTotalPlanQty(glueScheduleResult.getTotalPlanQty());
        result.setTotalSurplus(glueScheduleResult.getTotalSurplus());
        result.setRemark(glueScheduleResult.getRemark());

        result.setMidPlanQty(glueScheduleResult.getMidPlanQty());
        result.setMidProduceOrder(glueScheduleResult.getMidProduceOrder());
        result.setMidExpectStartTime(glueScheduleResult.getMidExpectStartTime());
        result.setMidExpectFinishTime(glueScheduleResult.getMidExpectFinishTime());
        result.setMidRemark(glueScheduleResult.getMidRemark());

        result.setNightPlanQty(glueScheduleResult.getNightPlanQty());
        result.setNightProduceOrder(glueScheduleResult.getNightProduceOrder());
        result.setNightExpectStartTime(glueScheduleResult.getNightExpectStartTime());
        result.setNightExpectFinishTime(glueScheduleResult.getNightExpectFinishTime());
        result.setNightRemark(glueScheduleResult.getNightRemark());

        result.setDayPlanQty(glueScheduleResult.getDayPlanQty());
        result.setDayProduceOrder(glueScheduleResult.getDayProduceOrder());
        result.setDayExpectStartTime(glueScheduleResult.getDayExpectStartTime());
        result.setDayExpectFinishTime(glueScheduleResult.getDayExpectFinishTime());
        result.setDayRemark(glueScheduleResult.getDayRemark());
        return result;
    }

    /**
     * 比较数值是否相同
     * @return 是否相同
     */
    private boolean compare(Integer d1, Integer d2) {
        return ObjectUtils.compare(d1, d2) == 0;
    }

    /**
     * 计算各个班次完成率并赋值
     * @param list 要操作的集合
     */
    private void setFinishRate(List<GlueScheduleResult> list) {
        for (GlueScheduleResult scheduleResult : list) {
            // 计算各班次完成率
            Double midPlanQty = scheduleResult.getMidPlanQty();
            Double midFinishQty = scheduleResult.getMidFinishQty();
            if (midPlanQty != null && midFinishQty != null && BigDecimal.ZERO.doubleValue() != midPlanQty) {
                scheduleResult.setMidFinishRate(BigDecimalUtil.div(midFinishQty, midPlanQty, 4));
            }
            Double nightPlanQty = scheduleResult.getNightPlanQty();
            Double nightFinishQty = scheduleResult.getNightFinishQty();
            if (nightPlanQty != null && nightFinishQty != null && BigDecimal.ZERO.doubleValue() != nightPlanQty) {
                scheduleResult.setNightFinishRate(BigDecimalUtil.div(nightFinishQty, nightPlanQty, 4));
            }
            Double dayPlanQty = scheduleResult.getDayPlanQty();
            Double dayFinishQty = scheduleResult.getDayFinishQty();
            if (dayPlanQty != null && dayFinishQty != null && BigDecimal.ZERO.doubleValue() != dayPlanQty) {
                scheduleResult.setDayFinishRate(BigDecimalUtil.div(dayFinishQty, dayPlanQty, 4));
            }
        }
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @Override
    public AjaxResult getSummaryVo(GlueScheduleResult scheduleResult) {
        ScheduleSummaryVo summaryVo = glueScheduleResultMapper.getSummaryVo(scheduleResult);
        if (summaryVo == null) {
            summaryVo = new ScheduleSummaryVo();
            summaryVo.setScheduleDate(scheduleResult.getScheduleDate());
        }
        ScheduleSummaryVo lastDayPlanQtySummaryVo = glueScheduleResultMapper.getLastDayPlanQty(scheduleResult);
        Double lastDayPlanQty = 0D;
        if (lastDayPlanQtySummaryVo != null) {
            lastDayPlanQty = lastDayPlanQtySummaryVo.getLastDayPlanQty();
            summaryVo.setLastDayPlanQty(lastDayPlanQty);
        }
        Double consumeQty = ObjectUtils.defaultIfNull(glueScheduleResultMapper.getConsume(scheduleResult), 0D);
        summaryVo.setCxConsumeQty(consumeQty);
        // 理论交班库存计算,理论交班库存 = 库存 + 昨日早班 + 夜班 - 终炼胶消耗量
        Double stockQty = ObjectUtils.defaultIfNull(summaryVo.getStockQty(), 0D);
        Double nightPlanQty = ObjectUtils.defaultIfNull(summaryVo.getNightPlanQty(), 0D);
        if (lastDayPlanQty != null && consumeQty != null) {
            summaryVo.setTheoreticClassStockQty(stockQty + lastDayPlanQty + nightPlanQty - consumeQty);
        }
        return AjaxResult.success(summaryVo);
    }
}
