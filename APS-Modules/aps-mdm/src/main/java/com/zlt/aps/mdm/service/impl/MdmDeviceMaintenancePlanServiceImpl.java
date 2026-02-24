package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.enums.MaintenancePlanTypeEnum;
import com.tlt.aps.enums.MdmMachineTypeEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.mdm.mapper.LhMachineInfoEntityMapper;
import com.zlt.aps.mdm.mapper.MdmDeviceMaintenancePlanEntityMapper;
import com.zlt.aps.mdm.mapper.MdmModelInfoEntityMapper;
import com.zlt.aps.mdm.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.mdm.service.IMdmDeviceMaintenancePlanService;
import com.zlt.aps.mdm.utils.LambdaWrapperBuilder;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmDeviceMaintenancePlan;
import com.zlt.aps.mdm.api.domain.entity.MdmModelInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mdm.api.domain.vo.MdmDeviceMaintenancePlanVo;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * 设备维护计划
 */
@Slf4j
@Service
public class MdmDeviceMaintenancePlanServiceImpl implements IMdmDeviceMaintenancePlanService {

    @Resource
    private MdmDeviceMaintenancePlanEntityMapper mdmDeviceMaintenancePlanEntityMapper;
    @Resource
    private MdmMoldingMachineEntityMapper moldingMachineEntityMapper;
    @Resource
    private LhMachineInfoEntityMapper lhMachineInfoEntityMapper;
    @Resource
    private MdmModelInfoEntityMapper mdmModelInfoEntityMapper;


    @Resource
    private BaseDao baseDao;

    /**
     * 根据多个主键删除记录
     *
     * @param ids 主键列表
     * @return 删除的记录数量
     */
    @Override
    public int deleteByIds(List<Long> ids) {
        return mdmDeviceMaintenancePlanEntityMapper.deleteBatchIds(ids);
    }

    /**
     * 新增设备维护计划
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(MdmDeviceMaintenancePlan docDeviceMaintenancePlan) {
        checkSafeData(docDeviceMaintenancePlan);
        return mdmDeviceMaintenancePlanEntityMapper.insert(docDeviceMaintenancePlan);
    }

    /**
     * 校验数据是否合法：校验机台编号、截断日期、校验日期周期合法、校验时间是否存在交叉
     */
    private void checkSafeData(MdmDeviceMaintenancePlan docDeviceMaintenancePlan) {
        // 校验机台编号
        checkMachineCode(docDeviceMaintenancePlan);
        // 截断日期
        truncateDate(Collections.singletonList(docDeviceMaintenancePlan));
        // 校验日期周期合法
        checkDateCycle(docDeviceMaintenancePlan);
        // 校验时间是否存在交叉
        checkCrossDate(docDeviceMaintenancePlan);
    }

    /**
     * 截断时间
     * 1、成型/硫化-具体到小时
     * 2、模具-具体到天
     */
    private void truncateDate(List<MdmDeviceMaintenancePlan> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        SimpleDateFormat hourFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");

        list.forEach(item -> {
            truncateDateItem(item, dayFormat, hourFormat);
        });
    }

    /**
     * 截断时间
     * 1、成型/硫化-具体到小时
     * 2、模具-具体到天
     */
    private void truncateDateItem(MdmDeviceMaintenancePlan item, SimpleDateFormat dayFormat, SimpleDateFormat hourFormat) {
        SimpleDateFormat itemFormat = null;
        if (MdmMachineTypeEnum.MODEL.getValue().equals(item.getMachineType())) {
            // 模具-开始时间和结束时间截断到天
            itemFormat = dayFormat;
        } else if (MdmMachineTypeEnum.MOLDING.getValue().equals(item.getMachineType())
                || MdmMachineTypeEnum.VULCANIZING.getValue().equals(item.getMachineType())
                || MdmMachineTypeEnum.CLEAN_MOLD.getValue().equals(item.getMachineType())) {
            // 成型/硫化-开始时间和结束时间截断到小时
            itemFormat = hourFormat;
        }

        if (itemFormat == null) {
            return;
        }

        try {
            if (item.getBeginDate() != null) {
                item.setBeginDate(itemFormat.parse(itemFormat.format(item.getBeginDate())));
            }
            if (item.getEndDay() != null) {
                item.setEndDay(itemFormat.parse(itemFormat.format(item.getEndDay())));
            }
        } catch (ParseException e) {
            log.error("日期截断异常", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断设备编号是否存在
     */
    private void checkMachineCode(MdmDeviceMaintenancePlan plan) {
        MdmDeviceMaintenancePlanVo planVo = new MdmDeviceMaintenancePlanVo();
        BeanUtils.copyProperties(plan, planVo);
        echoMachineId(Collections.singletonList(planVo));
        if (planVo.getMachineId() == null) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.noMachineInformation"));
        }
    }

    /**
     * 根据主键编辑设备维护计划
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateByPrimaryKey(MdmDeviceMaintenancePlan docDeviceMaintenancePlan) {
        checkSafeData(docDeviceMaintenancePlan);
        return mdmDeviceMaintenancePlanEntityMapper.updateById(docDeviceMaintenancePlan);
    }

    /**
     * 校验开始时间不能大于结束时间
     * 如果是成型和硫化还要校验开始时间不能等于结束时间
     */
    private void checkDateCycle(MdmDeviceMaintenancePlan plan) {
        if (plan.getBeginDate() == null || plan.getEndDay() == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.timeNull"));
        }
        if (plan.getBeginDate().after(plan.getEndDay())) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.timeCheck"));
        }

        // 如果是成型和硫化还要校验开始时间不能等于结束时间
        if (MdmMachineTypeEnum.MOLDING.getValue().equals(plan.getMachineType())
                || MdmMachineTypeEnum.VULCANIZING.getValue().equals(plan.getMachineType())
                || MdmMachineTypeEnum.CLEAN_MOLD.getValue().equals(plan.getMachineType())) {
            if (plan.getBeginDate().equals(plan.getEndDay())) {
                throw new BusinessException(I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.timeSample"));
            }
        }
    }

    /**
     * 判断开始时间和结束时间是否存在交叉
     */
    private void checkCrossDate(MdmDeviceMaintenancePlan plan) {
        // 开始时间和结束时间不等的交叉场景
        Long count = mdmDeviceMaintenancePlanEntityMapper.selectCount(Wrappers.lambdaQuery(MdmDeviceMaintenancePlan.class)
                .ne(plan.getId() != null, MdmDeviceMaintenancePlan::getId, plan.getId())
                .eq(MdmDeviceMaintenancePlan::getPlanType, plan.getPlanType())
                .eq(MdmDeviceMaintenancePlan::getFactoryCode, plan.getFactoryCode())
                .eq(MdmDeviceMaintenancePlan::getMachineType, plan.getMachineType())
                .eq(MdmDeviceMaintenancePlan::getMachineCode, plan.getMachineCode())
                .gt(MdmDeviceMaintenancePlan::getEndDay, plan.getBeginDate())
                .lt(MdmDeviceMaintenancePlan::getBeginDate, plan.getEndDay()));
        if (count > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.crossDate"));
        }

        // 如果是模具，前后时间和不能出现交叉
        if (MdmMachineTypeEnum.MODEL.getValue().equals(plan.getMachineType())) {
            count = mdmDeviceMaintenancePlanEntityMapper.selectCount(Wrappers.lambdaQuery(MdmDeviceMaintenancePlan.class)
                    .ne(plan.getId() != null, MdmDeviceMaintenancePlan::getId, plan.getId())
                    .eq(MdmDeviceMaintenancePlan::getPlanType, plan.getPlanType())
                    .eq(MdmDeviceMaintenancePlan::getFactoryCode, plan.getFactoryCode())
                    .eq(MdmDeviceMaintenancePlan::getMachineType, plan.getMachineType())
                    .eq(MdmDeviceMaintenancePlan::getMachineCode, plan.getMachineCode())
                    .ge(MdmDeviceMaintenancePlan::getEndDay, plan.getBeginDate())
                    .le(MdmDeviceMaintenancePlan::getBeginDate, plan.getEndDay()));
            // 开始时间和结束时间相等的交叉场景（主要是模具会配置相等的开始和结束）
            if (count > 0) {
                throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.crossDate"));
            }
        }
    }

    /**
     * 根据主键查询
     */
    @Override
    public MdmDeviceMaintenancePlan selectByPrimaryKey(Long id) {
        return mdmDeviceMaintenancePlanEntityMapper.selectById(id);
    }

    /**
     * 查询设备维护计划数据
     *
     * @param docDeviceMaintenancePlan
     * @return
     */
    @Override
    public List<MdmDeviceMaintenancePlan> selectDocDeviceMaintenancePlanList(MdmDeviceMaintenancePlanVo docDeviceMaintenancePlan) {
        LambdaQueryWrapper<MdmDeviceMaintenancePlan> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(docDeviceMaintenancePlan.getPlanType() != null, MdmDeviceMaintenancePlan::getPlanType, docDeviceMaintenancePlan.getPlanType());
        wrapper.eq(StringUtils.isNotBlank(docDeviceMaintenancePlan.getFactoryCode()), MdmDeviceMaintenancePlan::getFactoryCode, docDeviceMaintenancePlan.getFactoryCode());
        wrapper.eq(docDeviceMaintenancePlan.getYear() != null, MdmDeviceMaintenancePlan::getYear, docDeviceMaintenancePlan.getYear());
        wrapper.eq(docDeviceMaintenancePlan.getMonth() != null, MdmDeviceMaintenancePlan::getMonth, docDeviceMaintenancePlan.getMonth());
        if (MdmMachineTypeEnum.COMBINE_LH.getValue().equals(docDeviceMaintenancePlan.getMachineType())){
            //硫化+洗模组合
            Integer[] machineTypeArr = new Integer[]{MdmMachineTypeEnum.VULCANIZING.getValue(),MdmMachineTypeEnum.CLEAN_MOLD.getValue()};
            wrapper.in(MdmDeviceMaintenancePlan::getMachineType,machineTypeArr);
        }else{
            wrapper.eq(docDeviceMaintenancePlan.getMachineType() != null, MdmDeviceMaintenancePlan::getMachineType, docDeviceMaintenancePlan.getMachineType());
        }

        wrapper.ge(docDeviceMaintenancePlan.getBeginDate() != null, MdmDeviceMaintenancePlan::getBeginDate, docDeviceMaintenancePlan.getBeginDate());
        wrapper.le(docDeviceMaintenancePlan.getEndDay() != null, MdmDeviceMaintenancePlan::getEndDay, docDeviceMaintenancePlan.getEndDay());

        wrapper.like(docDeviceMaintenancePlan.getMachineCode() != null, MdmDeviceMaintenancePlan::getMachineCode, docDeviceMaintenancePlan.getMachineCode());
        return mdmDeviceMaintenancePlanEntityMapper.selectList(wrapper);
    }

    /**
     * 导入设备维护计划
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importData(List<MdmDeviceMaintenancePlanVo> list, boolean updateSupport, Long importLogId) {
        //1.初始化
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<MdmDeviceMaintenancePlan> insertList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat hourFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        // 判断时间交叉
        Function<MdmDeviceMaintenancePlan, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getPlanType(), v.getFactoryCode(), v.getMachineType(), v.getMachineCode());
        Map<String, List<MdmDeviceMaintenancePlan>> indexMap = new HashMap<>();

        //2.国际化初始化
        String noMachineInformationStr = I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.noMachineInformation");
        String timeCheck = I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.timeCheck");
        String yearAndMonthMustBeTheSame = I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.yearAndMonthMustBeTheSame");
        String crossDate = I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.crossDate");
        String timeSample = I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.timeSample");
        String modelRepairStr = I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.modelRepair");

        // 回显成型机、硫化机、模具信息
        echoMachineId(list);

        //3.公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmDeviceMaintenancePlanVo itemPlan = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, itemPlan);
            if (CollectionUtils.isNotEmpty(validated)) {
                itemPlan.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
                continue;
            }

            // 判断机台编号
            if (ObjectUtils.isEmpty(itemPlan.getMachineId())) {
                itemPlan.setId(-999L);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, noMachineInformationStr, importErrorLogs);
                continue;
            }

            // 如果是模具，只能有维修计划
            if (MaintenancePlanTypeEnum.MAINTENANCE.getCode().equals(itemPlan.getPlanType())
                    && MdmMachineTypeEnum.MODEL.getValue().equals(itemPlan.getMachineType())) {
                itemPlan.setId(-999L);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, modelRepairStr, importErrorLogs);
                continue;
            }

            // 截断时间
            truncateDateItem(itemPlan, dayFormat, hourFormat);
            // 判断开始时间不大于结束时间
            if (itemPlan.getBeginDate().after(itemPlan.getEndDay())) {
                itemPlan.setId(-999L);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, timeCheck, importErrorLogs);
                continue;
            }
            // 如果是成型和硫化还要校验开始时间不能等于结束时间
            if (MdmMachineTypeEnum.MOLDING.getValue().equals(itemPlan.getMachineType())
                    || MdmMachineTypeEnum.VULCANIZING.getValue().equals(itemPlan.getMachineType())
                    || MdmMachineTypeEnum.CLEAN_MOLD.getValue().equals(itemPlan.getMachineType())) {
                if (itemPlan.getBeginDate().equals(itemPlan.getEndDay())) {
                    itemPlan.setId(-999L);
                    failureNum++;
                    addImportErrorLog(importLogId, errorNum, timeSample, importErrorLogs);
                    continue;
                }
            }

            if (!checkYearMonth(calendar, itemPlan.getYear(), itemPlan.getMonth(), itemPlan.getBeginDate())
                    || !checkYearMonth(calendar, itemPlan.getYear(), itemPlan.getMonth(), itemPlan.getEndDay())) {
                itemPlan.setId(-999L);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, yearAndMonthMustBeTheSame, importErrorLogs);
                continue;
            }

            // 校验导入记录的维护时间是否存在交叉的记录
            String key = keyFunc.apply(itemPlan);
            List<MdmDeviceMaintenancePlan> indexList = indexMap.getOrDefault(key, new ArrayList<>());
            if (CollectionUtils.isNotEmpty(indexList)) {
                boolean crossTag = false;
                for (int j = indexList.size() - 1; j >= 0; j--) {
                    MdmDeviceMaintenancePlan otherPlan = indexList.get(j);
                    if (otherPlan.getBeginDate().before(itemPlan.getEndDay()) && otherPlan.getEndDay().after(itemPlan.getBeginDate())) {
                        crossTag = true;
                        otherPlan.setId(-999L);
                        failureNum++;
                        addImportErrorLog(importLogId, otherPlan.getRowNum(), crossDate, importErrorLogs);
                        indexList.remove(j);
                    } else if (otherPlan.getBeginDate().equals(itemPlan.getEndDay()) && otherPlan.getEndDay().equals(itemPlan.getBeginDate())) {
                        crossTag = true;
                        otherPlan.setId(-999L);
                        failureNum++;
                        addImportErrorLog(importLogId, otherPlan.getRowNum(), crossDate, importErrorLogs);
                        indexList.remove(j);
                    }
                }

                if (crossTag) {
                    itemPlan.setId(-999L);
                    failureNum++;
                    addImportErrorLog(importLogId, errorNum, crossDate, importErrorLogs);
                    continue;
                }
            }

            indexList.add(itemPlan);
            indexMap.put(key, indexList);
            itemPlan.setRowNum(errorNum);
            insertList.add(itemPlan);
        }

        // 过滤id不等于空的数据
        insertList = insertList.stream().filter(v -> v.getId() == null).collect(Collectors.toList());

        try {
            successNum = insertList.size();
            if(CollectionUtils.isNotEmpty(insertList)){
                // 删除历史数据
                deleteByList(insertList);
                // 插入新记录
                baseDao.insertBatch(insertList);
            }
        } catch (Exception e) {
            log.error("导入设备维护计划失败", e);
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 删除历史的数据-计划类型、分厂、年月、
     */
    private void deleteByList(List<MdmDeviceMaintenancePlan> insertList) {
        if(CollectionUtils.isEmpty(insertList)){
            return;
        }

        LambdaQueryWrapper<MdmDeviceMaintenancePlan> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(insertList, MdmDeviceMaintenancePlan::getPlanType,
                MdmDeviceMaintenancePlan::getFactoryCode,
                MdmDeviceMaintenancePlan::getYear,
                MdmDeviceMaintenancePlan::getMonth);

        mdmDeviceMaintenancePlanEntityMapper.delete(wrapper);
    }

    /**
     * 校验日期是否在指定年月
     */
    private boolean checkYearMonth(Calendar calendar, Integer year, Integer month, Date date) {
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR) == year && (calendar.get(Calendar.MONTH) + 1) == month;
    }

    /**
     * 回显成型机、硫化机、模具信息
     */
    private void echoMachineId(List<MdmDeviceMaintenancePlanVo> list) {
        List<MdmDeviceMaintenancePlanVo> planVoList = list.stream().filter(v -> v.getMachineType() != null && StringUtils.isNotBlank(v.getFactoryCode()) && StringUtils.isNotBlank(v.getMachineCode())).collect(Collectors.toList());
        // 成型机信息
        Map<Integer, List<MdmDeviceMaintenancePlanVo>> machineTypeMap = planVoList.stream().collect(Collectors.groupingBy(MdmDeviceMaintenancePlan::getMachineType));
        Map<String, Long> moldingMap = new HashMap<>();
        List<MdmDeviceMaintenancePlanVo> moldingList = machineTypeMap.get(MdmMachineTypeEnum.MOLDING.getValue());
        if (CollectionUtils.isNotEmpty(moldingList)) {
            List<String> factoryCodeList = moldingList.stream().map(MdmDeviceMaintenancePlanVo::getFactoryCode).distinct().collect(Collectors.toList());
            List<String> machineCodeList = moldingList.stream().map(MdmDeviceMaintenancePlanVo::getMachineCode).distinct().collect(Collectors.toList());
            List<MdmMoldingMachine> moldingMachineList = moldingMachineEntityMapper.selectList(Wrappers.lambdaQuery(MdmMoldingMachine.class)
                    .in(MdmMoldingMachine::getFactoryCode, factoryCodeList)
                    .in(MdmMoldingMachine::getCxMachineCode, machineCodeList));
            moldingMap = moldingMachineList.stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getCxMachineCode()), MdmMoldingMachine::getId, (v1, v2) -> v1));
        }
        // 硫化机信息
        Map<String, Long> vulcanizingMap = new HashMap<>();
        List<MdmDeviceMaintenancePlanVo> vulcanizingList = new ArrayList<>();
        List<MdmDeviceMaintenancePlanVo> vulcanizingTypeList = machineTypeMap.get(MdmMachineTypeEnum.VULCANIZING.getValue());
        if (CollectionUtils.isNotEmpty(vulcanizingTypeList)) {
            vulcanizingList.addAll(vulcanizingTypeList);
        }
        List<MdmDeviceMaintenancePlanVo> cleanMoldList = machineTypeMap.get(MdmMachineTypeEnum.CLEAN_MOLD.getValue());
        if (CollectionUtils.isNotEmpty(cleanMoldList)) {
            vulcanizingList.addAll(cleanMoldList);
        }
        if (CollectionUtils.isNotEmpty(vulcanizingList)) {
            List<String> factoryCodeList = vulcanizingList.stream().map(MdmDeviceMaintenancePlanVo::getFactoryCode).distinct().collect(Collectors.toList());
            List<String> machineCodeList = vulcanizingList.stream().map(MdmDeviceMaintenancePlanVo::getMachineCode).distinct().collect(Collectors.toList());
            List<LhMachineInfo> moldingMachineList = lhMachineInfoEntityMapper.selectList(Wrappers.lambdaQuery(LhMachineInfo.class)
                    .in(LhMachineInfo::getFactoryCode, factoryCodeList)
                    .in(LhMachineInfo::getMachineCode, machineCodeList));
            vulcanizingMap = moldingMachineList.stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMachineCode()), LhMachineInfo::getId, (v1, v2) -> v1));
        }
        // 模具信息
        Map<String, Long> modelMap = new HashMap<>();
        List<MdmDeviceMaintenancePlanVo> modelList = machineTypeMap.get(MdmMachineTypeEnum.MODEL.getValue());
        if (CollectionUtils.isNotEmpty(modelList)) {
            List<String> factoryCodeList = modelList.stream().map(MdmDeviceMaintenancePlanVo::getFactoryCode).distinct().collect(Collectors.toList());
            List<String> machineCodeList = modelList.stream().map(MdmDeviceMaintenancePlanVo::getMachineCode).distinct().collect(Collectors.toList());
            List<MdmModelInfo> moldingMachineList = mdmModelInfoEntityMapper.selectList(Wrappers.lambdaQuery(MdmModelInfo.class)
                    .in(MdmModelInfo::getFactoryCode, factoryCodeList)
                    .in(MdmModelInfo::getMouldCode, machineCodeList));
            modelMap = moldingMachineList.stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMouldCode()), MdmModelInfo::getId, (v1, v2) -> v1));
        }

        for (MdmDeviceMaintenancePlanVo planVo : planVoList) {
            if (MdmMachineTypeEnum.MOLDING.getValue().equals(planVo.getMachineType())) {
                planVo.setMachineId(moldingMap.get(GenerageMapKeyUtils.createMapKey(planVo.getFactoryCode(), planVo.getMachineCode())));
            } else if (MdmMachineTypeEnum.VULCANIZING.getValue().equals(planVo.getMachineType()) || MdmMachineTypeEnum.CLEAN_MOLD.getValue().equals(planVo.getMachineType())) {
                planVo.setMachineId(vulcanizingMap.get(GenerageMapKeyUtils.createMapKey(planVo.getFactoryCode(), planVo.getMachineCode())));
            } else if (MdmMachineTypeEnum.MODEL.getValue().equals(planVo.getMachineType())) {
                planVo.setMachineId(modelMap.get(GenerageMapKeyUtils.createMapKey(planVo.getFactoryCode(), planVo.getMachineCode())));
            }
        }
    }
}
