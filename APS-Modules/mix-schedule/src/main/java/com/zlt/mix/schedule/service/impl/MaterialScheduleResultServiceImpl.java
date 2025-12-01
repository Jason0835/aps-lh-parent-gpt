package com.zlt.mix.schedule.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.DictUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.common.core.utils.ExcelUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.schedule.api.domain.dto.*;
import com.zlt.mix.schedule.api.domain.entity.*;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.mapper.MaterialEngineMapper;
import com.zlt.mix.schedule.engine.mapper.RecipeEngineMapper;
import com.zlt.mix.schedule.engine.mapper.SchedulePublishEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.RecipeEngineService;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEnginePublishService;
import com.zlt.mix.schedule.engine.service.materialschedule.MaterialEngineService;
import com.zlt.mix.schedule.engine.util.CombinedMapKey;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.MaterialScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.MaterialSpanVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeWeightVo;
import com.zlt.mix.schedule.engine.vo.SchedulePublishLogVo;
import com.zlt.mix.schedule.mapper.GlueScheduleResultMapper;
import com.zlt.mix.schedule.mapper.MaterialScheduleResultMapper;
import com.zlt.mix.schedule.service.*;
import com.zlt.mix.setting.api.domain.entity.AccessoriesMachine;
import com.zlt.mix.setting.api.domain.entity.LhflGlueStock;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import com.zlt.mix.setting.api.service.ILhflGlueStockService;

import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
import static com.zlt.mix.common.core.utils.MixCommonUtil.compare;

/**
 * 硫化辅料日计划排程Service业务层处理
 *
 * @author chen
 * @date 2022-05-24
 */
@Service
public class MaterialScheduleResultServiceImpl extends ServiceImpl<MaterialScheduleResultMapper, MaterialScheduleResult> implements MaterialScheduleResultService {
    @Resource
    private MaterialScheduleResultMapper materialScheduleResultMapper;

    @Resource
    private GlueScheduleResultMapper glueScheduleResultMapper;

    @Resource
    private MaterialEngineService materialEngineService;

    @Resource
    private GlueScheduleEnginePublishService glueScheduleEnginePublishService;

    @Resource
    private ScheduleOperLogService scheduleOperLogService;

    @Resource
    private MaterialSpanSendService materialSpanSendService;
    @Resource
    private MaterialSpanReceiveService materialSpanReceiveService;
	@Autowired
	private SchedulePublishEngineMapper schedulePublishEngineMapper;
    @Resource
	private MaterialEngineMapper materialEngineMapper;
    @Resource
    private RecipeEngineMapper recipeEngineMapper;
    @Resource
    private ILhflGlueStockService lhflGlueStockService;
	@Resource
	private RecipeEngineService recipeEngineService;

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
	 * 发布类型：硫磺辅料排程发布
	 */
	private static final String SCHEDULE_TYPE_MATERIAL = "1";
	/**
	 * html换行符
	 */
	private static final String BR = "<br/>";


    /**
     * 查询硫化辅料日计划排程列表
     *
     * @param materialScheduleResult 硫化辅料日计划排程
     * @return 硫化辅料日计划排程
     */
    @Override
    public List<MaterialScheduleResult> selectMaterialScheduleResultList(MaterialScheduleResult materialScheduleResult) {
        List<LhflMachine> machineList = materialScheduleResultMapper.getMachineInfo(new LhflMachine());
        Map<String, LhflMachine> machineNameMap = machineList.stream().collect(Collectors.toMap(item -> item.getMixArea() + item.getMachineCode(), Function.identity()));
        List<MaterialScheduleResult> scheduleResults = materialScheduleResultMapper.selectMaterialScheduleResultList(materialScheduleResult);
        setMachineNameAndClassShift(machineNameMap, scheduleResults);
        this.setReleaseStatusTip(materialScheduleResult, scheduleResults);
        return scheduleResults;
    }
    
    /**
     * 设置各班发布状态提示信息
     * @param list
     */
	private void setReleaseStatusTip(MaterialScheduleResult materialScheduleResult, List<MaterialScheduleResult> list) {
		Date scheduleDate = materialScheduleResult.getScheduleDate();
		String mixArea = materialScheduleResult.getMixArea();
		// 查当日本密炼区失败的发布记录
		Map<String, String> errorMessageMap = schedulePublishEngineMapper
				.listLatestPublishLog(DateUtil.formatDate(scheduleDate), mixArea, SCHEDULE_TYPE_MATERIAL, WS_CODE_ERROR)
				.stream().filter(s -> s.getOrderNo() != null && s.getRemark() != null)
				.collect(Collectors.toMap(SchedulePublishLogVo::getOrderNo, SchedulePublishLogVo::getRemark));

		// 设置各班发布状态提示信息
		String midErrorMessage = I18nUtil.getMessage("schedule.publish.error.mid");
		String nightErrorMessage = I18nUtil.getMessage("schedule.publish.error.night");
		String dayErrorMessage = I18nUtil.getMessage("schedule.publish.error.day");
		String timeOutErrorMessage = I18nUtil.getMessage("schedule.publish.error.mes.timeOut");
		for (MaterialScheduleResult result : list) {
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
     * 保存硫化辅料日计划排程信息（id为空则新增，id不为空则修改）
     *
     * @param materialScheduleResult
     */
    @Override
    public List<MaterialScheduleResult> saveMaterialScheduleResult(MaterialScheduleResult materialScheduleResult) {
        /*if (ZltConstant.NOT_UNIQUE.equals(checkMaterialScheduleResultUnique(materialScheduleResult))) {
            throw new RuntimeException(I18nUtil.getMessage("schedule.materialScheduleResult.database.unique"));
        }*/
    	this.checkProduceOrderRepeat(materialScheduleResult); // 校验不可重复
        List<MaterialScheduleResult> list = new ArrayList<>();
        MaterialScheduleResult oldScheduleResult = null;
        materialScheduleResult.setBaseValue(materialScheduleResult.getId());
        String operIp = materialScheduleResult.getOperIp(); // 操作IP
        if (materialScheduleResult.getId() != null) {
            setTotalPlanQty(materialScheduleResult);
            oldScheduleResult = getById(materialScheduleResult.getId());
            boolean isChangeSchedule = !this.compareScheduleField(oldScheduleResult, materialScheduleResult);
            boolean isChangeRemark = !this.compareRemarkField(oldScheduleResult, materialScheduleResult);
            // 如果有修改，更改发布状态为待发布
            if (isChangeSchedule || isChangeRemark) {
                // 排程调量操作日志，有修改计划量，才记录日志
                if (!(compare(oldScheduleResult.getMidPlanQty(), materialScheduleResult.getMidPlanQty()) && compare(oldScheduleResult.getNightPlanQty(), materialScheduleResult.getNightPlanQty())
                        && compare(oldScheduleResult.getDayPlanQty(), materialScheduleResult.getDayPlanQty()))) {
                    saveScheduleOperLog(materialScheduleResult, oldScheduleResult, ZltConstant.OPER_TYPE_CHANGE_PLAN, operIp);
                }
                // 排程调序操作日志，有修改顺序，才记录日志
                if (!(compare(oldScheduleResult.getMidProduceOrder(), materialScheduleResult.getMidProduceOrder()) && compare(oldScheduleResult.getNightProduceOrder(), materialScheduleResult.getNightProduceOrder())
                        && compare(oldScheduleResult.getDayProduceOrder(), materialScheduleResult.getDayProduceOrder()))) {
                    saveScheduleOperLog(materialScheduleResult, oldScheduleResult, ZltConstant.OPER_TYPE_SEQUENCING, operIp);
                }
                String releaseStatus = oldScheduleResult.getReleaseStatus(); // 发布状态，默认为原发布状态
                if (isChangeSchedule) { // 如果有修改排程属性，则需要重算发布状态与预计时间等；如果只修改了备注，则不需要调整
            		releaseStatus = materialScheduleResult.getPublishSuccessCount() == 0 ? ZltConstant.NO_RELEASE : ZltConstant.WAIT_RELEASING;
            	}
                materialScheduleResult.setReleaseStatus(releaseStatus);
                materialScheduleResultMapper.updateScheduleResult(materialScheduleResult);
                if (isChangeSchedule) {
	                list = this.retrySchedule(oldScheduleResult, materialScheduleResult);  //调用引擎
            	} else {
            		list.add(materialScheduleResult);
            	}
            }
        } else {
            list = materialEngineService.addEngineSchedule(materialScheduleResult);
            // 排程插单操作日志
            saveScheduleOperLog(materialScheduleResult, null, ZltConstant.OPER_TYPE_INSERT_ORDER, operIp);
            this.saveOrUpdateBatch(list);
        }
        if (list.size() > 0) {
            List<LhflMachine> machineList = materialScheduleResultMapper.getMachineInfo(new LhflMachine());
            Map<String, LhflMachine> machineNameMap = machineList.stream().collect(Collectors.toMap(item -> item.getMixArea() + item.getMachineCode(), Function.identity()));
            list = materialScheduleResultMapper.selectByIds(list.stream().map(MaterialScheduleResult::getId).collect(Collectors.toList()));
            setMachineNameAndClassShift(machineNameMap, list);
        }
        return list;
    }

    /**
     * 校验生产顺序不能重复
     * @param schedule
     */
	private void checkProduceOrderRepeat(MaterialScheduleResult schedule) {
		MaterialScheduleResult scheduleParam = new MaterialScheduleResult();
    	scheduleParam.setScheduleDate(schedule.getScheduleDate());
    	scheduleParam.setMixArea(schedule.getMixArea());
    	scheduleParam.setMachineCode(schedule.getMachineCode());
        List<MaterialScheduleResultVo> scheduleList = materialEngineMapper.listMaterialSchedule(scheduleParam, Arrays.asList(schedule.getMachineCode())); //查询出和待校验排程相同机台的记录
    	if (schedule.getId() != null) { // 如果是修改，需要过滤掉原记录
    		scheduleList = scheduleList.stream().filter(s -> !s.getId().equals(schedule.getId())).collect(Collectors.toList());
    	}
        materialEngineService.checkProduceOrderRepeat(schedule, scheduleList); // 校验生产顺序不可重复
	}

    /**
     * 批量删除硫化辅料日计划排程
     *
     * @param ids 需要删除的硫化辅料日计划排程ID
     * @return 结果
     */
    @Override
    public int deleteMaterialScheduleResultByIds(Long[] ids) {
        return materialScheduleResultMapper.deleteMaterialScheduleResultByIds(ids);
    }


    /**
     * 校验硫化辅料日计划排程唯一性
     */
    @Override
    public String checkMaterialScheduleResultUnique(MaterialScheduleResult materialScheduleResult) {
        if (materialScheduleResult == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<MaterialScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MaterialScheduleResult::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(MaterialScheduleResult::getScheduleDate, materialScheduleResult.getScheduleDate());
        queryWrapper.eq(MaterialScheduleResult::getMachineCode, materialScheduleResult.getMachineCode());
        queryWrapper.eq(MaterialScheduleResult::getMaterialName, materialScheduleResult.getMaterialName());
        if (materialScheduleResult.getId() != null) {
            queryWrapper.ne(MaterialScheduleResult::getId, materialScheduleResult.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<MaterialScheduleResult> list = materialScheduleResultMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入硫化辅料日计划排程数据
     *
     * @param list          要导入的数据集合
     * @param scheduleDate 排程日期
     * @param mixArea      密炼区
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MaterialScheduleResult> list, Date scheduleDate, String mixArea, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MaterialScheduleResult> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Integer> importRowMap = new HashMap<>();//通过校验后的数据与在原本的Excel中对应的行数

        try {
            // 机台数据，用于转换机台名称为机台编号保存
            AccessoriesMachine param = new AccessoriesMachine();
            param.setMixArea(mixArea);
            List<AccessoriesMachine> machineList = materialScheduleResultMapper.getAccessoriesMachineList(param);
            if (CollectionUtils.isEmpty(machineList)) {
                String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
                addImportErrorLog(importLogId, null, message, importErrorLogs);
                return AjaxResult.error(message, importErrorLogs);
            }
            Map<String, AccessoriesMachine> machineMap = machineList.stream().collect(Collectors.toMap(item -> item.getMaterialName() + item.getMachineName(), Function.identity()));

            // 查询配方表配方类型和配方类型名称对应map，用于转换
            List<RecipeType> recipeTypeInfo = glueScheduleResultMapper.getRecipeTypeInfo(new RecipeType());
            Map<String, String> recipeTypeInfoMap = recipeTypeInfo.stream().collect(Collectors.toMap(RecipeType::getRecipeTypeName, RecipeType::getRecipeTypeCode, (s, s2) -> s));
            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
//            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getScheduleDate() + a.getMaterialName() + a.getMachineName(), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                MaterialScheduleResult materialScheduleResult = list.get(i);
                int errorNum = i + 3;
                // 发布状态默认未发布
                if (StringUtil.isBlank(materialScheduleResult.getReleaseStatus())) {
                    materialScheduleResult.setReleaseStatus(ZltConstant.NO_RELEASE);
                }
                //excel中重复记录校验
                /*Long hasValue = groupMap.get(materialScheduleResult.getScheduleDate() + materialScheduleResult.getMaterialName() + materialScheduleResult.getMachineName());
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    materialScheduleResult.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("schedule.materialScheduleResult.excel.unique");
                    addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                }*/

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, materialScheduleResult); //校验excel每个单元格长度、类型等

                // 机台名称 转为机台code
                String machineName = materialScheduleResult.getMachineName();
                String materialName = materialScheduleResult.getMaterialName();
                AccessoriesMachine machineInfo = machineMap.getOrDefault(materialName + machineName, new AccessoriesMachine());
                String machineCode = machineInfo.getMachineCode();
                if (StringUtil.isBlank(machineCode)) {
                    String message = String.format(I18nUtil.getMessage("schedule.materialScheduleResult.message.import.machineNotExist"), mixArea, machineName, materialName);
                    addImportErrorLog(importLogId, errorNum, message, validated);
                }
                // 配方类型名称 转为配方类型code
                String recipeTypeName = materialScheduleResult.getRecipeTypeName();
                if (StringUtil.isNotBlank(recipeTypeName) && recipeTypeInfoMap.get(recipeTypeName) == null) {
                    String message = String.format(I18nUtil.getMessage("schedule.materialScheduleResult.message.import.recipeTypeNotExist"), recipeTypeName);
                    addImportErrorLog(importLogId, errorNum, message, validated);
                }
                // 根据机台班制，限制各班输入是否合法
                Integer classShift = machineInfo.getClassShift();
                if (ZltConstant.CLASS_SHIFT_LONG_DAY.equals(classShift)) {
                    // 长白班，中夜班的计划量、顺序、备注不可导入
                    Double midPlanQty = Double.valueOf(0).equals(materialScheduleResult.getMidPlanQty()) ? null : materialScheduleResult.getMidPlanQty();
                    Integer midProduceOrder = Integer.valueOf(0).equals(materialScheduleResult.getMidProduceOrder()) ? null : materialScheduleResult.getMidProduceOrder();
                    String midRemark = StringUtil.isBlank(materialScheduleResult.getMidRemark()) ? null : materialScheduleResult.getMidRemark();
                    Double nightPlanQty = Double.valueOf(0).equals(materialScheduleResult.getNightPlanQty()) ? null : materialScheduleResult.getNightPlanQty();
                    Integer nightProduceOrder = Integer.valueOf(0).equals(materialScheduleResult.getNightProduceOrder()) ? null : materialScheduleResult.getNightProduceOrder();
                    String nightRemark = StringUtil.isBlank(materialScheduleResult.getNightRemark()) ? null : materialScheduleResult.getNightRemark();
                    if (ObjectUtils.anyNotNull(midPlanQty, midProduceOrder, midRemark, nightPlanQty, nightProduceOrder, nightRemark)) {
                        String message = String.format(I18nUtil.getMessage("schedule.materialScheduleResult.import.classShiftFilter"), machineName, DictUtils.getLabel("LH_CLASS_SHIFT", classShift.toString()),
                                I18nUtil.getMessage("schedule.materialScheduleResult.mid") + "," + I18nUtil.getMessage("schedule.materialScheduleResult.night"));
                        addImportErrorLog(importLogId, errorNum, message, validated);
                    }
                }else if (ZltConstant.CLASS_SHIFT_TWO_SHIFT.equals(classShift)) {
                    // 两班制，白班的计划量、顺序、备注不可导入
                    Double dayPlanQty = Double.valueOf(0).equals(materialScheduleResult.getDayPlanQty()) ? null : materialScheduleResult.getDayPlanQty();
                    Integer dayProduceOrder = Integer.valueOf(0).equals(materialScheduleResult.getDayProduceOrder()) ? null : materialScheduleResult.getDayProduceOrder();
                    String dayRemark = StringUtil.isBlank(materialScheduleResult.getDayRemark()) ? null : materialScheduleResult.getDayRemark();
                    if (ObjectUtils.anyNotNull(dayPlanQty, dayProduceOrder, dayRemark)) {
                        String message = String.format(I18nUtil.getMessage("schedule.materialScheduleResult.import.classShiftFilter"), machineName, DictUtils.getLabel("LH_CLASS_SHIFT", classShift.toString()),
                                I18nUtil.getMessage("schedule.materialScheduleResult.day"));
                        addImportErrorLog(importLogId, errorNum, message, validated);
                    }
                }

                if (CollectionUtils.isEmpty(validated) && materialScheduleResult.getId() == null) {
                    materialScheduleResult.setBaseValue(null);
                    materialScheduleResult.setScheduleDate(scheduleDate);
                    materialScheduleResult.setMixArea(mixArea);
                    materialScheduleResult.setMachineCode(machineCode);
                    materialScheduleResult.setRecipeType(recipeTypeInfoMap.get(recipeTypeName));
                    materialScheduleResult.setDataSource(ZltConstant.MATERIAL_SCHEDULE_SOURCE_IMPORT);
                    importRowMap.put(importList.size(), i + 2);
                    importList.add(materialScheduleResult);
                } else {
                    materialScheduleResult.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            if (CollectionUtils.isNotEmpty(importList)) {
                /*codeUniqueErrorLogs = materialScheduleResultMapper.listMaterialScheduleResultNotUnique(importList, importLogId,
                        I18nUtil.getMessage("schedule.materialScheduleResult.database.unique"), SecurityUtils.getUsername());
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

            if (CollectionUtils.isNotEmpty(importList)) {
                this.materialEngineService.batchAddEngineSchedule(scheduleDate, mixArea, importList);   //调用引擎导入接口
            }
//            log.debug("=======================硫磺辅料日计划导入集合信息start=========================");
//            log.debug(importList.toString());
//            if (CollectionUtils.isNotEmpty(importList)) {
//                MaterialScheduleResult materialScheduleResult = new MaterialScheduleResult();
//                materialScheduleResult.setScheduleDate(scheduleDate);
//                materialScheduleResultMapper.deleteByScheduleDate(materialScheduleResult);
//                materialScheduleResultMapper.mergeSql(importList);
//            }
//            log.debug("=======================硫磺辅料日计划导入集合信息end=========================");

        } catch (Exception e) {
            log.error("导入出错", e);
            // 执行sql失败，插入导入失败记录
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = importList.size();  //成功记录数
        failureNum = list.size() - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 发布硫磺辅料日计划
     */
    @Override
    public AjaxResult publish(MaterialScheduleResult scheduleResult) {
        //对应的日计划是否存在机台为空、多个机台的数据，不允许发布
        AjaxResult ajaxResult = AjaxResult.error(I18nUtil.getMessage("ui.frame.alter.mustChooseOneRecord"));
        List<Long> ids = Arrays.asList(Convert.toLongArray(scheduleResult.getIds()));
        if(ids.size()>0){
			List<MaterialScheduleResult> materialScheduleResultList = materialScheduleResultMapper.selectBatchIds(ids);
			if (CollectionUtil.isEmpty(materialScheduleResultList)) {
				return AjaxResult.error(I18nUtil.getMessage("schedule.materialScheduleResult.norecord"));
			}
			for (MaterialScheduleResult result : materialScheduleResultList) {
				String releaseState = result.getReleaseStatus();
				if (!ZltConstant.NO_RELEASE.equals(releaseState) && !ZltConstant.WAIT_RELEASING.equals(releaseState)
						&& !ZltConstant.FAILURE_RELEASE.equals(releaseState)) {
					return AjaxResult.error(I18nUtil.getMessage("schedule.materialScheduleResult.publish.status"));
				}
			}
			
			// 记录原先的排程发布状态
			Map<Long, String> oldReleaseStatusMap = materialScheduleResultList.stream()
					.filter(s -> s.getReleaseStatus() != null).collect(Collectors.toMap(MaterialScheduleResult::getId,
							MaterialScheduleResult::getReleaseStatus, (s1, s2) -> s1));
			
			ajaxResult = glueScheduleEnginePublishService.publishMaterialScheduleResult(materialScheduleResultList);
			this.updateBatchById(materialScheduleResultList, 100);

			// 记录发布日志
			List<ScheduleOperLog> operLogs = new ArrayList<>();
			String operIp = scheduleResult.getOperIp(); // 操作IP
//			List<MaterialScheduleResult> newScheduleResultList = materialScheduleResultMapper.selectBatchIds(ids);
			for (MaterialScheduleResult schedule: materialScheduleResultList) {
				Long newResultId = schedule.getId();
				MaterialScheduleResult oldSchedule = new MaterialScheduleResult();
				BeanUtils.copyProperties(schedule, oldSchedule);
				String releaseStatus = oldReleaseStatusMap.get(newResultId);
				if (StringUtils.isNotEmpty(releaseStatus)) {
					oldSchedule.setReleaseStatus(releaseStatus);
				}
				ScheduleOperLog scheduleOperLog = this.buildScheduleOperLog(schedule, oldSchedule, ZltConstant.OPER_TYPE_PUBLISH, operIp);
				operLogs.add(scheduleOperLog);
			}
	        if (CollectionUtils.isNotEmpty(operLogs)) {
	            scheduleOperLogService.batchInsertScheduleOperLogInfo(operLogs);
	        }
        }
        return ajaxResult;
    }

    /**
     * 批量转机台
     *
     * @param machineCode 要转到的机台编号
     * @param ids 批量转的排程id(前端已经改造，转机台只能单个转，不能支持批量)
     */
    @Override
    public AjaxResult batchChangeMachine(String machineCode, Long[] ids) {
        // 根据id查询对应记录，校验是否唯一
        MaterialScheduleResult oldSchedule = this.getById(ids[0]);  //转机台前的排产信息
        MaterialScheduleResult newSchedule = new MaterialScheduleResult();   //转机台后的排程信息
        BeanUtils.copyProperties(oldSchedule, newSchedule);
        if (!compare(machineCode, oldSchedule.getMachineCode())) {
            newSchedule.setReleaseStatus(newSchedule.getPublishSuccessCount() == null || newSchedule.getPublishSuccessCount() == 0 ? ZltConstant.NO_RELEASE : ZltConstant.WAIT_RELEASING);
            newSchedule.setMachineCode(machineCode);

            newSchedule = materialEngineService.retryMachine(oldSchedule, newSchedule);  //调用引擎转机台接口，转机台后默认把转机台的排产放到最后，并重新计算顺序和预计完成时间
            // 排程转机台操作日志
            saveScheduleOperLog(newSchedule, oldSchedule, ZltConstant.OPER_TYPE_CHANGE_MACHINE, null);
            this.saveOrUpdate(newSchedule);
        }
        return AjaxResult.success();
    }

    /**
     * 转机台（新）：转机台后重新创建一个新的工单号
     * @param scheduleResult
     */
    public void changeMachine(MaterialScheduleResult scheduleResult) {
        List<MaterialScheduleResult> list = materialScheduleResultMapper.selectByIds(Arrays.asList(scheduleResult.getId()));
        MaterialScheduleResult oldScheduleResult = list.get(0);  //转机台前的排程信息

        //转机台（新）。转机台后，创建新的排产记录；之前的记录保留。新机台上的各班计划量=原计划量 -  完成量
        List<MaterialScheduleResult> resultList = materialEngineService.retryMachineNew(oldScheduleResult, scheduleResult);
        if(resultList != null && !resultList.isEmpty()) {
        	// 排程转机台操作日志
            saveScheduleOperLog(scheduleResult, oldScheduleResult, ZltConstant.OPER_TYPE_CHANGE_MACHINE, scheduleResult.getOperIp());
            this.saveOrUpdateBatch(resultList);
        }
    }


    /**
     * 修改了各班计划量，修改了顺序。都需要把此机台下的排产重新进行计算
     * @param oldSchedule
     * @param newSchedule
     */
    private List<MaterialScheduleResult> retrySchedule(MaterialScheduleResult oldSchedule, MaterialScheduleResult newSchedule) {
        List<MaterialScheduleResult> list = this.materialEngineService.retrySchedule(oldSchedule, newSchedule);
        if (CollectionUtils.isNotEmpty(list)) {
            this.updateBatchById(list);
        }
        return list;
    }

    /**
     * 比较排产属性值是否有更改
     *
     * @param oldScheduleResult  数据库内数据
     * @param scheduleResult 前端接收数据
     * @return 是否相同
     */
    private boolean compareScheduleField(MaterialScheduleResult oldScheduleResult, MaterialScheduleResult scheduleResult) {
        boolean flag = compare(oldScheduleResult.getMidPlanQty(), scheduleResult.getMidPlanQty());
        flag = flag && compare(oldScheduleResult.getDayPlanQty(), scheduleResult.getDayPlanQty());
        flag = flag && compare(oldScheduleResult.getNightPlanQty(), scheduleResult.getNightPlanQty());
        flag = flag && compare(oldScheduleResult.getMidProduceOrder(), scheduleResult.getMidProduceOrder());
        flag = flag && compare(oldScheduleResult.getDayProduceOrder(), scheduleResult.getDayProduceOrder());
        flag = flag && compare(oldScheduleResult.getNightProduceOrder(), scheduleResult.getNightProduceOrder());
        return flag;
    }

    /**
     * 比较属性值是否有更改
     *
     * @param oldScheduleResult  数据库内数据
     * @param scheduleResult 前端接收数据
     * @return 是否相同
     */
    private boolean compareRemarkField(MaterialScheduleResult oldScheduleResult, MaterialScheduleResult scheduleResult) {
        boolean flag = compare(oldScheduleResult.getRemark(), scheduleResult.getRemark());
        flag = flag && compare(oldScheduleResult.getMidRemark(), scheduleResult.getMidRemark());
        flag = flag && compare(oldScheduleResult.getDayRemark(), scheduleResult.getDayRemark());
        flag = flag && compare(oldScheduleResult.getNightRemark(), scheduleResult.getNightRemark());
        return flag;
    }

    /**
     * 根据模板文件导出到Excel
     *
     * @param dto 参数
     * @return Excel字节数组
     */
    @Override
    public byte[] exportData(MaterialScheduleResultExportDictDto dto) {
        List<MaterialScheduleResult> list = this.selectMaterialScheduleResultList(dto);
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + I18nUtil.getMessage("schedule.materialScheduleResult.modelName") + ".xlsx");
        Workbook webBook = ExcelUtils.readExcel(in);
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, String> recipeStageDictMap = dto.getRecipeStageDictMap();
            Map<String, String> mixAreaDictMap = dto.getMixAreaDictMap();
            Map<String, String> releaseStatusDictMap = dto.getReleaseStatusDictMap();
            Map<String, String> isOrNotDictMap = dto.getIsOrNotDictMap();
            Map<String, String> classShiftDictMap = dto.getClassShiftDictMap();
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            CellStyle percentCellStyle = ExcelUtils.createCellStyle(webBook);//创建单元格百分比格式
            percentCellStyle.setDataFormat(webBook.createDataFormat().getFormat("0.0%"));
            for (int i = 0; i < list.size(); i++) {
                MaterialScheduleResult materialScheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
                int cellNum = 0;
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getMachineName() == null ? "" : materialScheduleResult.getMachineName());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getClassShift() == null ? "" : classShiftDictMap.getOrDefault(String.valueOf(materialScheduleResult.getClassShift()), ""));
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getReleaseStatus() == null ? "" : releaseStatusDictMap.getOrDefault(materialScheduleResult.getReleaseStatus(), ""));
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getMaterialName() == null ? "" : materialScheduleResult.getMaterialName());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getRecipeTypeName() == null ? "" : materialScheduleResult.getRecipeTypeName());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getRecipeVersionId() == null ? "" : materialScheduleResult.getRecipeVersionId());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getRecipeStage() == null ? "" : recipeStageDictMap.getOrDefault(materialScheduleResult.getRecipeStage(), ""));
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getStockQty() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getStockQty());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getSafeStockQty() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getSafeStockQty());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getDemandQty() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getDemandQty());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getDemandPlanning() == null ? "" : materialScheduleResult.getDemandPlanning());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getTotalPlanQty() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getTotalPlanQty());
//                row.createCell(cellNum++).setCellValue(materialScheduleResult.getTotalSurplus() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getTotalSurplus());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getMidProduceOrder() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getMidProduceOrder());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getMidPlanQty() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getMidPlanQty());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getMidFinishQty() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getMidFinishQty());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getMidFinishRate() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getMidFinishRate());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getMidExpectStartTime() == null ? "" : DateFormatUtils.format(materialScheduleResult.getMidExpectStartTime(), "yyyy-MM-dd HH:mm:ss"));
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getMidExpectFinishTime() == null ? "" : DateFormatUtils.format(materialScheduleResult.getMidExpectFinishTime(), "yyyy-MM-dd HH:mm:ss"));
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getMidRemark() == null ? "" : materialScheduleResult.getMidRemark());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getNightProduceOrder() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getNightProduceOrder());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getNightPlanQty() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getNightPlanQty());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getNightFinishQty() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getNightFinishQty());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getNightFinishRate() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getNightFinishRate());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getNightExpectStartTime() == null ? "" : DateFormatUtils.format(materialScheduleResult.getNightExpectStartTime(), "yyyy-MM-dd HH:mm:ss"));
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getNightExpectFinishTime() == null ? "" : DateFormatUtils.format(materialScheduleResult.getNightExpectFinishTime(), "yyyy-MM-dd HH:mm:ss"));
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getNightRemark() == null ? "" : materialScheduleResult.getNightRemark());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getDayProduceOrder() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getDayProduceOrder());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getDayPlanQty() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getDayPlanQty());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getDayFinishQty() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getDayFinishQty());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getDayFinishRate() == null ? BigDecimal.ZERO.doubleValue() : materialScheduleResult.getDayFinishRate());
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getDayExpectStartTime() == null ? "" : DateFormatUtils.format(materialScheduleResult.getDayExpectStartTime(), "yyyy-MM-dd HH:mm:ss"));
                row.createCell(cellNum++).setCellValue(materialScheduleResult.getDayExpectFinishTime() == null ? "" : DateFormatUtils.format(materialScheduleResult.getDayExpectFinishTime(), "yyyy-MM-dd HH:mm:ss"));
                row.createCell(cellNum).setCellValue(materialScheduleResult.getDayRemark() == null ? "" : materialScheduleResult.getDayRemark());
                for (int j = 0; j <= cellNum; j++) {
                    // 完成率列设置百分比格式
                    if (j == 15 || j == 22 || j == 29) {
                        row.getCell(j).setCellStyle(percentCellStyle);
                        continue;
                    }
                    row.getCell(j).setCellStyle(cellStyle);
                }
            }
            Cell cell = sheet.getRow(0).getCell(0);
            Date scheduleDate = dto.getScheduleDate();
            String title = String.format(I18nUtil.getMessage("schedule.materialScheduleResult.tableTitle"),
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
     * 检测对应日期和密炼区的数据是否存在
     *
     * @param scheduleResult 日期和密炼区
     * @return 是否唯一的常量值
     */
    @Override
    public String checkScheduleDateAndMixAreaExist(MaterialScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<MaterialScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MaterialScheduleResult::getScheduleDate, scheduleResult.getScheduleDate());
        queryWrapper.eq(MaterialScheduleResult::getMixArea, scheduleResult.getMixArea());
        queryWrapper.eq(MaterialScheduleResult::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);

        Long integer = materialScheduleResultMapper.selectCount(queryWrapper);
        if (integer != null && integer > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 更改配方信息
     *
     * @param materialScheduleResult id、配方阶段、配方版本号、配方类型
     * @return 结果
     */
    @Override
    public AjaxResult changeRecipe(MaterialScheduleResult materialScheduleResult) {
        //比较配方阶段、配方版本号、配方类型是否修改了
        MaterialScheduleResult oldScheduleResult = materialScheduleResultMapper.selectById(materialScheduleResult.getId());
        boolean flag = compare(oldScheduleResult.getRecipeStage(), materialScheduleResult.getRecipeStage());
        flag = flag && compare(oldScheduleResult.getRecipeVersionId(), materialScheduleResult.getRecipeVersionId());
        flag = flag && compare(oldScheduleResult.getRecipeType(), materialScheduleResult.getRecipeType());
        if (!flag) {
            materialScheduleResult.setReleaseStatus(oldScheduleResult.getPublishSuccessCount() == 0 ? ZltConstant.NO_RELEASE : ZltConstant.WAIT_RELEASING);
        }

        materialScheduleResultMapper.changeRecipe(materialScheduleResult);
        //返回发布状态，以便于页面显示
        // TODO 调用更改配方信息接口
        return AjaxResult.success(I18nUtil.getMessage("common.msg.ajax.operation.success"), materialScheduleResult.getReleaseStatus());
    }

    /**
     * 根据id查询排程结果信息
     * @param id id
     * @return 查询到的记录
     */
    @Override
    public MaterialScheduleResult getById(Long id) {
        MaterialScheduleResult result = materialScheduleResultMapper.getById(id);
        if (result == null) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.noRecord"));
        }
        LhflMachine param = new LhflMachine();
        param.setMixArea(result.getMixArea());
        List<LhflMachine> machineList = materialScheduleResultMapper.getMachineInfo(param);
        Map<String, LhflMachine> machineNameMap = machineList.stream().collect(Collectors.toMap(item -> item.getMixArea() + item.getMachineCode(), Function.identity()));
        setMachineNameAndClassShift(machineNameMap, Collections.singletonList(result));
        return result;
    }

    /**
     * 根据ids查询发布状态是否有不是【未发布】的记录
     * @param ids ids
     * @return 不是未发布的记录数
     */
    @Override
    public int isNoReleaseByIds(Long[] ids) {
        return materialScheduleResultMapper.isNoReleaseByIds(ids);
    }

    /**
     * 根据参数查询机台信息
     *
     * @param param
     */
    @Override
    public List<LhflMachine> getMachineInfo(LhflMachine param) {
        return materialScheduleResultMapper.getMachineInfo(param);
    }

    /**
     * 计算总计划量
     * @param materialScheduleResult 中班、夜班、白班计划量总和计算
     */
    private void setTotalPlanQty(MaterialScheduleResult materialScheduleResult) {
        Double midPlanQty = materialScheduleResult.getMidPlanQty() == null ? 0 : materialScheduleResult.getMidPlanQty();
        Double nightPlanQty = materialScheduleResult.getNightPlanQty() == null ? 0 : materialScheduleResult.getNightPlanQty();
        Double dayPlanQty = materialScheduleResult.getDayPlanQty() == null ? 0 : materialScheduleResult.getDayPlanQty();
        materialScheduleResult.setTotalPlanQty(BigDecimalUtil.add(midPlanQty, nightPlanQty, dayPlanQty));
    }

    /**
     * 根据密炼区+机台code回填机台名称、班制，如果有多个机台名称则班制为空，计算各个班次完成率并赋值
     * @param machineNameMap 机台map
     * @param scheduleResults 要回填的集合
     */
    private void setMachineNameAndClassShift(Map<String, LhflMachine> machineNameMap, List<MaterialScheduleResult> scheduleResults) {
        // 根据密炼区+机台code回填机台名称、班制，如果有多个机台名称则班制为空，计算各个班次完成率并赋值
        for (MaterialScheduleResult scheduleResult : scheduleResults) {
            String mixArea = scheduleResult.getMixArea();
            String machineCodeStr = scheduleResult.getMachineCode();
            if (StringUtil.isNotBlank(machineCodeStr)) {
                if (machineCodeStr.contains(",")) {
                    String[] machineCodeArr = machineCodeStr.split(",");
                    StringBuilder machineNameSb = new StringBuilder();
                    for (String machineCode : machineCodeArr) {
                        String key = mixArea + machineCode;
                        LhflMachine machine = machineNameMap.get(key);
                        if (ObjectUtils.isNotEmpty(machine)) {
                            machineNameSb.append(machine.getMachineName()).append(",");
                        }
                    }
                    scheduleResult.setMachineName(machineNameSb.substring(0, machineNameSb.length() - 1));
                }else {
                    String key = mixArea + machineCodeStr;
                    LhflMachine machine = machineNameMap.get(key);
                    if (ObjectUtils.isNotEmpty(machine)) {
                        scheduleResult.setMachineName(machine.getMachineName());
                        if(scheduleResult.getClassShift() == null) {
                            scheduleResult.setClassShift(machine.getClassShift());
                        }
                    }
                }
            }
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
     * 保存排程操作日志
     * @param newScheduleResult 操作后的排程操作日志
     * @param operType 操作类型
     */
	private void saveScheduleOperLog(MaterialScheduleResult newScheduleResult,
			MaterialScheduleResult oldMaterialScheduleResult, String operType, String operIp) {
		ScheduleOperLog scheduleOperLog = this.buildScheduleOperLog(newScheduleResult, oldMaterialScheduleResult,
				operType, operIp);
		scheduleOperLogService.saveScheduleOperLog(scheduleOperLog);
	}

	/**
	 * 构建排程操作日志对象
	 * @param newScheduleResult
	 * @param oldMaterialScheduleResult
	 * @param operType
	 * @param operIp
	 * @return
	 */
	private ScheduleOperLog buildScheduleOperLog(MaterialScheduleResult newScheduleResult,
			MaterialScheduleResult oldMaterialScheduleResult, String operType, String operIp) {
		ScheduleOperLog scheduleOperLog = new ScheduleOperLog();
        scheduleOperLog.setScheduleType(ZltConstant.OPER_SCHEDULE_TYPE_MATERIAL);
        scheduleOperLog.setScheduleId(newScheduleResult.getId());
        scheduleOperLog.setOperType(operType);
        scheduleOperLog.setOperIp(operIp);
        scheduleOperLog.setScheduleDate(newScheduleResult.getScheduleDate());
        scheduleOperLog.setMixArea(newScheduleResult.getMixArea());
        scheduleOperLog.setMaterialCode(newScheduleResult.getMaterialName());

        if (!ZltConstant.OPER_TYPE_INSERT_ORDER.equals(operType) && !ZltConstant.OPER_TYPE_AUTO_SCHEDULE.equals(operType)) {
            MaterialScheduleResult oldScheduleResult = oldMaterialScheduleResult == null ? materialScheduleResultMapper.getById(newScheduleResult.getId()) : oldMaterialScheduleResult;
        	scheduleOperLog.setBeforeReleaseStatus(oldScheduleResult.getReleaseStatus());
            scheduleOperLog.setBeforeMachineCode(oldScheduleResult.getMachineCode());
            scheduleOperLog.setBeforeRecipeType(oldScheduleResult.getRecipeType());
            scheduleOperLog.setBeforeRecipeVersionId(oldScheduleResult.getRecipeVersionId());
            scheduleOperLog.setBeforeRecipeStage(oldScheduleResult.getRecipeStage());
            Double midPlanQty = oldScheduleResult.getMidPlanQty();
            scheduleOperLog.setBeforeMidPlan(ObjectUtils.isEmpty(midPlanQty) ? BigDecimal.ZERO.doubleValue() : midPlanQty);
            Integer midProduceOrder = oldScheduleResult.getMidProduceOrder();
            scheduleOperLog.setBeforeMidOrder(ObjectUtils.isEmpty(midProduceOrder) ? BigDecimal.ZERO.intValue() : midProduceOrder);
            Double nightPlanQty = oldScheduleResult.getNightPlanQty();
            scheduleOperLog.setBeforeNightPlan(ObjectUtils.isEmpty(nightPlanQty) ? BigDecimal.ZERO.doubleValue() : nightPlanQty);
            Integer nightProduceOrder = oldScheduleResult.getNightProduceOrder();
            scheduleOperLog.setBeforeNightOrder(ObjectUtils.isEmpty(nightProduceOrder) ? BigDecimal.ZERO.intValue() : nightProduceOrder);
            Double dayPlanQty = oldScheduleResult.getDayPlanQty();
            scheduleOperLog.setBeforeDayPlan(ObjectUtils.isEmpty(dayPlanQty) ? BigDecimal.ZERO.doubleValue() : dayPlanQty);
            Integer dayProduceOrder = oldScheduleResult.getDayProduceOrder();
            scheduleOperLog.setBeforeDayOrder(ObjectUtils.isEmpty(dayProduceOrder) ? BigDecimal.ZERO.intValue() : dayProduceOrder);
        }
        
    	scheduleOperLog.setAfterReleaseStatus(newScheduleResult.getReleaseStatus());
        scheduleOperLog.setAfterMachineCode(newScheduleResult.getMachineCode());
        scheduleOperLog.setAfterRecipeType(newScheduleResult.getRecipeType());
        scheduleOperLog.setAfterRecipeVersionId(newScheduleResult.getRecipeVersionId());
        scheduleOperLog.setAfterRecipeStage(newScheduleResult.getRecipeStage());
        Double midPlanQty = newScheduleResult.getMidPlanQty();
        scheduleOperLog.setAfterMidPlan(ObjectUtils.isEmpty(midPlanQty) ? BigDecimal.ZERO.doubleValue() : midPlanQty);
        Integer midProduceOrder = newScheduleResult.getMidProduceOrder();
        scheduleOperLog.setAfterMidOrder(ObjectUtils.isEmpty(midProduceOrder) ? BigDecimal.ZERO.intValue() : midProduceOrder);
        Double nightPlanQty = newScheduleResult.getNightPlanQty();
        scheduleOperLog.setAfterNightPlan(ObjectUtils.isEmpty(nightPlanQty) ? BigDecimal.ZERO.doubleValue() : nightPlanQty);
        Integer nightProduceOrder = newScheduleResult.getNightProduceOrder();
        scheduleOperLog.setAfterNightOrder(ObjectUtils.isEmpty(nightProduceOrder) ? BigDecimal.ZERO.intValue() : nightProduceOrder);
        Double dayPlanQty = newScheduleResult.getDayPlanQty();
        scheduleOperLog.setAfterDayPlan(ObjectUtils.isEmpty(dayPlanQty) ? BigDecimal.ZERO.doubleValue() : dayPlanQty);
        Integer dayProduceOrder = newScheduleResult.getDayProduceOrder();
        scheduleOperLog.setAfterDayOrder(ObjectUtils.isEmpty(dayProduceOrder) ? BigDecimal.ZERO.intValue() : dayProduceOrder);
        scheduleOperLog.setBaseValue(null);
		return scheduleOperLog;
	}

    /**
     * 获取统计信息
     * @param materialScheduleResult 日期、密炼区、机台编号
     * @return 统计好的信息列表
     */
    @Override
    public List<MaterialScheduleResultStatisticsDto> statistics(MaterialScheduleResult materialScheduleResult) {
        List<MaterialScheduleResultStatisticsDto> statistics = materialScheduleResultMapper.statistics(materialScheduleResult);
        //回显分厂没有提报
        if(CollectionUtils.isNotEmpty(statistics)){
            MaterialScheduleResultStatisticsDto materialScheduleResultStatisticsDto = statistics.get(statistics.size() - 1);
            if(StringUtil.isEmpty(materialScheduleResultStatisticsDto.getMachineName())){
                materialScheduleResultStatisticsDto.setMachineName(I18nUtil.getMessage("schedule.materialScheduleResult.defaultMachineName"));
            }
        }
        return statistics;
    }
    
    /**
     * 获取超期预警信息
     * @param materialScheduleResult
     * @return
     */
    @Override
	public List<MaterialExpireWarningDto> expireWarning(MaterialScheduleResult materialScheduleResult) {
		// 取出当天库存
		Date currentDate = DateUtil.thatDay(DateUtil.now());
		Date stockDate = currentDate;
		Date scheduleDate = DateUtils.addDays(currentDate, 1); // 排产日，明天
		String mixArea = materialScheduleResult.getMixArea();
		LhflGlueStock lhflGlueStock = new LhflGlueStock();
		lhflGlueStock.setMixArea(mixArea);
		lhflGlueStock.setStockDate(stockDate);
		List<LhflGlueStock> stockList = lhflGlueStockService.selectLhflGlueStock(lhflGlueStock);
		if (CollectionUtil.isEmpty(stockList)) { // 如果没有，则取上一天的库存
			stockDate = DateUtils.addDays(currentDate, -1); // 库存日期提前一天
			scheduleDate = currentDate; // 排程日期提前一天
			lhflGlueStock.setStockDate(stockDate);
			stockList = lhflGlueStockService.selectLhflGlueStock(lhflGlueStock);
		}
		if (CollectionUtil.isEmpty(stockList)) { // 如果依旧没有取到，则返回空列表
			return new ArrayList<>(0);
		}
		Date warningTime = DateUtils.addHours(scheduleDate, 16); // 库存预警时间 = 排产日的16点
		stockList = stockList.stream()
				// 过滤掉无效的库存数据
				.filter(s -> BigDecimalUtil.gtZero(s.getStockNum()))
				// 保留过期时间在库存预警时间之内的库存
				.filter(s -> s.getValidTime().compareTo(warningTime) <= 0)
				// 按到期时间顺序排序
				.sorted(Comparator.comparing(LhflGlueStock::getValidTime, Comparator.nullsLast(Date::compareTo)))
				.collect(Collectors.toList());

		if (CollectionUtil.isEmpty(stockList)) { // 如果库存没有有效数据，则返回空列表
			return new ArrayList<>(0);
		}

		// 统计前天白班与当当天三个班的小料消耗量
		Map<String, BigDecimal> materialConsumeMap = new HashMap<>();
		this.statisticsMaterialConsume(materialConsumeMap, stockDate, mixArea, GlueScheduleResult::getDayPlanQty); // 根据胶料排程结果与配方计算出各硫磺辅料的白班消耗量
		this.statisticsMaterialConsume(materialConsumeMap, scheduleDate, mixArea, GlueScheduleResult::getTotalPlanQty); // 根据胶料排程结果与配方计算出各硫磺辅料的排产日消耗量

		// 加载硫磺辅料配方
		MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
		Set<String> materialNameSet = new HashSet<>();
		materialNameSet.addAll(stockList.stream().map(LhflGlueStock::getMaterialName).collect(Collectors.toSet())); // 合并硫磺辅料库存物料
		materialNameSet.addAll(materialConsumeMap.keySet()); // 合并硫磺辅料库存日消耗物料
		recipeParams.setRecipeMaterialNameList(new ArrayList<>(materialNameSet));
		recipeParams.setMixArea(mixArea);
		List<MesPmtRecipeVo> recipeList = recipeEngineService.listGlueRecipe(recipeParams); // 根据密炼区 + 物料名称加载配方数据
		Map<String, MesPmtRecipeVo> recipeTypeMap = recipeList.stream()
				.sorted(Comparator.comparing(MesPmtRecipeVo::getRecipeType, Comparator.nullsLast(String::compareTo)))
				.collect(Collectors.toMap(MesPmtRecipeVo::getRecipeMaterialName, Function.identity(), (v1, v2) -> v1)); // 按物料名称取最接近ZZ的配方类型

		List<MaterialExpireWarningDto> expireWarningList = new ArrayList<>();
		for (LhflGlueStock stock : stockList) {
			String materialName = stock.getMaterialName();
			BigDecimal stockNum = stock.getStockNum();
			BigDecimal previousDayConsumeQty = materialConsumeMap.getOrDefault(materialName, BigDecimal.ZERO); // 取出小料的消耗量
			BigDecimal surplusStockNum = stockNum.subtract(previousDayConsumeQty); // 剩余库存=库存量-消耗量
			if (BigDecimalUtil.gtZero(surplusStockNum)) { // 如果剩余库存消耗不完，则需要生成预警
				MaterialExpireWarningDto expireWarning = new MaterialExpireWarningDto();
				expireWarning.setMixArea(mixArea);
				expireWarning.setMaterialName(materialName);
				expireWarning.setWarningQty(surplusStockNum);
				BigDecimal warningWeight = surplusStockNum.compareTo(stockNum) == 0 ? stock.getStockWeight()
						: this.caculateWeight(materialName, recipeTypeMap, surplusStockNum); // 预警重量，如果剩余库存等于小料库存，则直接使用库存重量，否则要通过配方重算
				expireWarning.setWarningWeight(warningWeight);
				expireWarning.setValidTime(stock.getValidTime());
				expireWarningList.add(expireWarning);
				materialConsumeMap.put(materialName, BigDecimal.ZERO); // 消耗量清空，到期时间在此之后的库存均需要预警
			} else {
				materialConsumeMap.put(materialName, surplusStockNum.abs()); // 库存可消耗完，则更新消耗量为剩余库存的绝对值
			}
		}

		return expireWarningList;
	}

    /**
     * 根据指定排产日的排产信息统计消耗的小料数量
     * @param stockDate
     * @param mixArea
     * @param recipeMap
     * @return
     */
	private void statisticsMaterialConsume(Map<String, BigDecimal> materialConsumeMap, Date stockDate, String mixArea,
			Function<GlueScheduleResult, Double> statisticsFunction) {
		// 获取库存日期的已发布终炼母炼排程
		GlueScheduleResult glueScheduleResult = new GlueScheduleResult();
		glueScheduleResult.setMixArea(mixArea);
		glueScheduleResult.setScheduleDate(stockDate);
		glueScheduleResult.setPublishSuccessCount(1);
		List<GlueScheduleResult> previousGlueScheduleResultList = glueScheduleResultMapper
				.selectGlueScheduleResultList(glueScheduleResult);

		// 有排程数据，则需要加载这些排程胶料的配方信息
		Map<CombinedMapKey, MesPmtRecipeVo> recipeMap;
		if (CollectionUtils.isNotEmpty(previousGlueScheduleResultList)) {
			MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
			recipeParams.setRecipeMaterialNameList(previousGlueScheduleResultList.stream()
					.map(GlueScheduleResult::getGlue).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
			recipeParams.setMixArea(mixArea);
			List<MesPmtRecipeVo> recipeList = recipeEngineService.listGlueRecipe(recipeParams); // 根据密炼区 + 物料名称加载配方数据
			recipeMap = recipeList.stream().collect(Collectors.toMap(
					r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode(), r.getRecipeType()),
					Function.identity(), (v1, v2) -> v1));
		} else {
			recipeMap = new HashMap<>();
		}

		// 根据称重配方统计小料的消耗量
		for (GlueScheduleResult schedule : previousGlueScheduleResultList) {
			MesPmtRecipeVo recipe = recipeMap.get(
					CombinedMapKey.createKey(schedule.getGlue(), schedule.getMachineCode(), schedule.getRecipeType()));
			if (recipe == null) { // 找不到配方跳过
				continue;
			}
			List<MesPmtRecipeWeightVo> recipeWeightList = recipe.getRecipeWeightList();
			if (recipeWeightList == null) { // 没有称重配方也跳过
				continue;
			}
			recipeWeightList.stream()
					// 过滤出硫磺辅料的称重配方
					.filter(r -> GlueEngineConstants.MAJOR_TYPE_XL.equals(r.getMajorType()))
					// 统计小料的需求量
					.forEach(r -> {
						String materialName = r.getRecipeMaterialName(); // 小料名称
						BigDecimal requireQty = materialConsumeMap.getOrDefault(materialName, BigDecimal.ZERO); // 取出同小料的需求量
						requireQty = BigDecimalUtil.valueOfZero(statisticsFunction.apply(schedule)).add(requireQty); // 需求车数=胶料车数
						materialConsumeMap.put(materialName, requireQty);
					});
		}
	}
    
    /**
     * 硫磺辅料车数换算成重量
     * @param recpie	硫磺辅料名称
     * @param materialQty	硫磺辅料车数
     * @return
     */
    private BigDecimal caculateWeight(String materialName, Map<String, MesPmtRecipeVo> recpieMap, BigDecimal materialQty) {
    	MesPmtRecipeVo recpie = recpieMap.get(materialName);
    	if (recpie != null && recpie.getLotTotalWeight() != null) {
    		return BigDecimalUtil.valueOf(recpie.getLotTotalWeight()).multiply(materialQty);
    	}
    	return BigDecimal.ZERO;
    }

    /**
     * 根据条件查询终炼母炼日计划跨区发送列表
     *
     * @param entity 查询条件
     * @return 结果
     */
    @Override
    public List<MaterialSpanSend> listMaterialSpanSend(MaterialSpanSend entity) {
        return materialSpanSendService.listMaterialSpanSend(entity);
    }

    /**
     * 发送跨区请求
     *
     * @param dto 跨区请求集合
     * @return 结果
     */
    @Override
    public AjaxResult sendMaterialSpan(MaterialSpanSendDto dto) throws ParseException {
        List<MaterialSpanSend> list = dto.getMaterialSpanSendList();
        if (CollectionUtils.isNotEmpty(list)) {
            // 日期校验
            Date currentDate = DateUtils.getNowDate(DateUtils.YYYY_MM_DD);
            for (MaterialSpanSend glueSpanSend : list) {
            	Date scheduleDate = glueSpanSend.getScheduleDate();
            	if (scheduleDate == null || scheduleDate.compareTo(currentDate) < 0) {
            		return AjaxResult.error("排程日期不可早于今天！");
            	}
            }
        	
            List<MaterialSpanReceive> materialSpanReceiveList = new ArrayList<>();
            for (MaterialSpanSend materialSpanSend : list) {
                materialSpanSend.setBaseValue(null);
//                materialSpanSend.setScheduleDate(DateUtils.addDays(DateUtils.getNowDate("yyyy-MM-dd"), 1));
                materialSpanSend.setSendTime(new Date());
                materialSpanSend.setReceiveStatus(ZltConstant.RECEIVE_STATUS_NO);
                materialSpanSend.setIsAuto(ZltConstant.IS_AUTO_NO);
                MaterialSpanReceive materialSpanReceive = new MaterialSpanReceive();
                BeanUtils.copyProperties(materialSpanSend, materialSpanReceive);
                materialSpanSendService.insertMaterialSpanSend(materialSpanSend);
                materialSpanReceive.setSendId(materialSpanSend.getId());
                materialSpanReceiveList.add(materialSpanReceive);
            }
            materialSpanReceiveService.batchInsertMaterialSpanReceive(materialSpanReceiveList);
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
    public List<MaterialSpanReceive> listMaterialSpanReceive(MaterialSpanReceive entity) {
        return materialSpanReceiveService.listMaterialSpanReceive(entity);
    }

    /**
     * 接收跨区请求
     *
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @Override
    public AjaxResult receiveMaterialSpanReceive(MaterialSpanReceiveDto dto) {
        List<MaterialSpanReceive> receiveList = dto.getMaterialSpanReceiveList();
        Long[] ids = new Long[receiveList.size()];
        for (int i = 0; i < receiveList.size(); i++) {
            MaterialSpanReceive materialSpanReceive = receiveList.get(i);
            materialSpanReceive.setBaseValue(materialSpanReceive.getId());
            materialSpanReceive.setReceiveStatus(ZltConstant.RECEIVE_STATUS_YES);
            materialSpanReceive.setReceiveTime(new Date());
            ids[i] = materialSpanReceive.getId();
        }
        if (materialSpanReceiveService.getAlreadyReceivedCountByIds(ids) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.receive.alreadyReceive"));
        }
        MaterialSpanReceive spanReceive = CollectionUtil.firstElement(receiveList);
        LhflMachine param = new LhflMachine();
        param.setMixArea(spanReceive.getEntrustedMixArea());
        
        // 判断机台状态，机台不可用的需要提醒
		Map<String, LhflMachine> machineMap = materialScheduleResultMapper.getMachineInfo(param).stream()
				.collect(Collectors.toMap(LhflMachine::getMachineCode, Function.identity(), (m1, m2) -> m1));
		for (MaterialSpanReceive materialSpanReceive : receiveList) {
			String machineCode = materialSpanReceive.getMachineCode();
			String machineName = materialSpanReceive.getMachineName();
			LhflMachine machine = machineMap.get(machineCode);
			if (machine == null || !ZltConstant.STATUS_ENABLE.equals(machine.getStatus())
					|| (!ZltConstant.STATUS_ENABLE.equals(machine.getMidStatus())
							&& !ZltConstant.STATUS_ENABLE.equals(machine.getNightStatus())
							&& !ZltConstant.STATUS_ENABLE.equals(machine.getDayStatus()))) {
				throw new RuntimeException(StringUtils.format(I18nUtil.getMessage("ui.data.message.receive.machine.error"), machineName));
			}
		}
		
        materialSpanReceiveService.mergeMaterialSpanReceive(receiveList);
        materialSpanSendService.mergeMaterialSpanSend(receiveList);

        MaterialSpanReceive receive = receiveList.get(0);
        this.saveMaterialSchedule(receive, ids);  //创建硫磺辅料排程记录
        return AjaxResult.success();
    }

    /**
     * 当密炼区当天已经进行了硫磺辅料自动排程后，再去接收跨区的硫磺辅料的生产计划，此时接收的数据都会被安排到对应机台的最后去
     * @param receive
     * @param ids
     */
    private void saveMaterialSchedule(MaterialSpanReceive receive, Long[] ids) {
        //调用引擎接口
        String mixArea = receive.getEntrustedMixArea();  //被委托密炼区
        Date scheduelDate = receive.getScheduleDate(); //排程日期
        List<MaterialScheduleResult> list = materialEngineService.spanReceivedEngine(mixArea, scheduelDate, Arrays.asList(ids));
        if(list != null && !list.isEmpty()) {
            this.saveOrUpdateBatch(list);
        }
    }

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     *
     * @param scheduleResult 参数
     * @return 结果
     */
    @Override
    public MaterialSpanReceiveQtyDto getSumQtyByMachineCode(MaterialScheduleResult scheduleResult) {
        return materialScheduleResultMapper.getSumQtyByMachineCode(scheduleResult);
    }

    /**
     * 根据id删除跨区发送记录
     * @param ids id
     * @return 结果
     */
    @Override
    public AjaxResult deleteMaterialSpanSend(Long[] ids) {
        // 校验是否已接收
        Integer alreadyReceivedCount = materialSpanSendService.getAlreadyReceivedCount(ids);
        if (alreadyReceivedCount > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.send.alreadyReceive"));
        }
        if (materialSpanReceiveService.getAlreadyReceivedCount(ids) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.send.alreadyReceive"));
        }
        materialSpanSendService.deleteByIds(ids);
        materialSpanReceiveService.deleteBySendIds(ids);
        return AjaxResult.success();
    }

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     *
     * @param ids 选中的id
     * @return 查询结果
     */
    @Override
    public List<MaterialScheduleResult> selectSpanSendNeedFieldByIds(Long[] ids) {
    	List<MaterialScheduleResult> spanList = materialScheduleResultMapper.selectSpanSendNeedFieldByIds(ids);
    	if (CollectionUtils.isNotEmpty(spanList)) {
    		List<String> materialList = spanList.stream().map(MaterialScheduleResult::getMaterialName).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    		if (CollectionUtils.isEmpty(materialList)) {
    			return spanList;
    		}
    		Map<String, List<MesPmtRecipeVo>> recipeMap = recipeEngineMapper.listLhflMachineRecipe(null, materialList)
    				.stream().collect(Collectors.groupingBy(MesPmtRecipeVo::getRecipeMaterialName)); // 查询物料配方机台关系
    		spanList.forEach(span -> {
    			String materialName = span.getMaterialName();
    			List<MesPmtRecipeVo> recipeList = recipeMap.get(materialName);
    			if (CollectionUtil.isEmpty(recipeList)) {
    				return;
    			}
    			if (recipeList.stream().map(MesPmtRecipeVo::getMixArea).filter(Objects::nonNull).distinct().count() > 1) {
    				return; // 判断是否有多个密炼区，多个密炼区则直接返回
    			}
    			
    			MesPmtRecipeVo recipe = CollectionUtil.firstElement(recipeList);
    			if (recipe != null) {
    				span.setMixArea(recipe.getMixArea());
    			}
    		});
    	}
    	return spanList;
    }

    /**
     * 自动排程后，根据跨区设置表，自动生产相应的跨区发送和接收记录
     *
     * @param mixArea      密炼区
     * @param scheduleDate 排程日期
     */
    public void autoCreateMaterialSpanRecord(String mixArea, Date scheduleDate) {
        materialSpanSendService.deleteNotReceived(mixArea, scheduleDate);   //删除还未接收的跨区发送记录
        materialSpanReceiveService.deleteNotReceived(mixArea, scheduleDate);   //删除还未接收的跨区接收记录

        MaterialSpanVo materialSpanVo = this.materialEngineService.autoCreateSpanRecord(mixArea, scheduleDate);  //从引擎接口那获取解析好的跨区发送接收记录
        if (materialSpanVo == null) {
            return;
        }

        List<MaterialSpanSend> spanSendList = materialSpanVo.getSpanSendList();  //跨区发送记录
        List<MaterialSpanReceive> spanReceiveList = materialSpanVo.getSpanReceiveList();  //跨区接收记录
        for (int i = 0; i < spanSendList.size(); i++) {
            //此处因为数据量很小，所以在循环里面调用数据库新增操作
            MaterialSpanSend spanSend = spanSendList.get(i);
            this.materialSpanSendService.save(spanSend);  //保存跨区发送记录

            MaterialSpanReceive spanReceive = spanReceiveList.get(i);
            spanReceive.setSendId(spanSend.getId());
            this.materialSpanReceiveService.save(spanReceive);
        }
    }

    /**
     * 保存自动排程日志
     * @param scheduleResult 参数
     * @return 结果
     */
    @Override
    public void saveAutoScheduleLog(MaterialScheduleResult scheduleResult) {
		// 记录自动排程布日志
		List<ScheduleOperLog> operLogs = new ArrayList<>();
		String operIp = scheduleResult.getOperIp(); // 操作IP
		List<MaterialScheduleResult> newScheduleResultList = materialScheduleResultMapper.selectMaterialScheduleResultList(scheduleResult);
		for (MaterialScheduleResult schedule: newScheduleResultList) {
			ScheduleOperLog scheduleOperLog = this.buildScheduleOperLog(schedule, null, ZltConstant.OPER_TYPE_AUTO_SCHEDULE, operIp);
			operLogs.add(scheduleOperLog);
		}
        if (CollectionUtils.isNotEmpty(operLogs)) {
            scheduleOperLogService.batchInsertScheduleOperLogInfo(operLogs);
        }
    }
}
