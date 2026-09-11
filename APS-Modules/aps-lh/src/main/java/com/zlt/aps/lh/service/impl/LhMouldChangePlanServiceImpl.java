package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import com.zlt.aps.lh.api.domain.entity.LhSharedMouldPat;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.component.OrderNoGenerator;
import com.zlt.aps.lh.mapper.LhMachineOnlineInfoMapper;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhSharedMouldPatEntityMapper;
import com.zlt.aps.lh.service.ILhMouldChangePlanService;
import com.zlt.aps.lh.service.ILhParamsService;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.maindata.mapper.LhMachineInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模具交替计划Service实现
 *
 * @author APS Team
 * @since 2026/04/01
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMouldChangePlanServiceImpl extends AbstractDocService<LhMouldChangePlan> implements ILhMouldChangePlanService {

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanMapper;

    @Resource
    private LhMachineOnlineInfoMapper lhMachineOnlineInfoMapper;

    @Resource
    private LhSharedMouldPatEntityMapper lhSharedMouldPatEntityMapper;

    @Autowired
    private ILhParamsService lhParamsService;

    @Autowired
    private LhMachineInfoEntityMapper lhMachineInfoEntityMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;


    @Autowired
    private RedisService redisService;

    @Autowired
    private IMesItfService mesItfService;

    @Autowired
    private OrderNoGenerator orderNoGenerator;

    @Override
    public String[] getQueryFormulas() {
        return new String[]{
                "lhMachineName->getcolvalue(T_LH_MACHINE_INFO, MACHINE_NAME, MACHINE_CODE, lhMachineCode)",
                "beforeMaterialDesc->getcolvalue(T_MDM_MATERIAL_INFO, MATERIAL_DESC, MATERIAL_CODE, beforeMaterialCode)",
                "afterMaterialDesc->getcolvalue(T_MDM_MATERIAL_INFO, MATERIAL_DESC, MATERIAL_CODE, afterMaterialCode)"
        };
    }

    /**
     * 解析模具交替计划最终展示的模具号。
     *
     * <p>该方法集中复用导出模具号取值规则，换活字块计划取在机模具号，
     * 其他计划取共享模具花纹配置，供导出展示和库表回写使用。</p>
     *
     * <p>按计划记录的分厂和排程日期分组匹配来源数据，并将计划原模具号与匹配模具号
     * 按原值优先的顺序取并集后回填至原列表。</p>
     *
     * @param planList 模具交替计划列表
     * @return 回填最终模具号后的模具交替计划列表
     */
    @Override
    public List<LhMouldChangePlan> resolveMouldCode(List<LhMouldChangePlan> planList) {
        if (CollectionUtils.isEmpty(planList)) {
            return planList == null ? Collections.emptyList() : planList;
        }

        Map<String, Map<String, List<LhMouldChangePlan>>> factorySchedulePlanMap = planList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(plan -> StringUtils.defaultIfBlank(plan.getFactoryCode(), ""),
                        LinkedHashMap::new, Collectors.groupingBy(plan -> plan.getScheduleDate() == null
                                        ? "" : DateUtil.formatDate(plan.getScheduleDate()),
                                LinkedHashMap::new, Collectors.toList())));
        for (Map.Entry<String, Map<String, List<LhMouldChangePlan>>> factoryEntry : factorySchedulePlanMap.entrySet()) {
            String currentFactoryCode = factoryEntry.getKey();
            for (List<LhMouldChangePlan> currentPlanList : factoryEntry.getValue().values()) {
                Date currentScheduleDate = currentPlanList.get(0).getScheduleDate() == null ? null
                        : DateUtil.beginOfDay(currentPlanList.get(0).getScheduleDate());
                Map<String, List<LhMachineOnlineInfo>> onlineInfoMap =
                        this.buildOnlineInfoMap(currentPlanList, currentFactoryCode, currentScheduleDate);
                Map<String, List<LhSharedMouldPat>> sharedMouldPatMap =
                        this.buildSharedMouldPatMap(currentPlanList, currentFactoryCode);
                for (LhMouldChangePlan plan : currentPlanList) {
                    String matchedMouldCode = MouldChangeTypeEnum.containsCode(
                            plan.getChangeMouldType(), MouldChangeTypeEnum.TYPE_BLOCK.getCode())
                            ? this.resolveOnlineMouldCode(plan, onlineInfoMap)
                            : this.resolveSharedMouldCode(plan, sharedMouldPatMap);
                    plan.setMouldCode(this.mergeMouldCode(plan.getMouldCode(), matchedMouldCode,
                            plan.getChangeMouldType()));
                }
            }
        }
        return planList;
    }

    /**
     * 刷新指定分厂、排程日期和硫化结果批次的模具号。
     *
     * @param factoryCode     工厂编码
     * @param scheduleDate    排程日期
     * @param lhResultBatchNo 硫化结果批次号
     */
    @Override
    public void refreshMouldCode(String factoryCode, Date scheduleDate, String lhResultBatchNo) {
        if (StringUtils.isBlank(factoryCode) || scheduleDate == null || StringUtils.isBlank(lhResultBatchNo)) {
            return;
        }
        List<LhMouldChangePlan> planList = lhMouldChangePlanMapper.selectList(
                new LambdaQueryWrapper<LhMouldChangePlan>()
                        .eq(LhMouldChangePlan::getFactoryCode, factoryCode)
                        .eq(LhMouldChangePlan::getScheduleDate, scheduleDate)
                        .eq(LhMouldChangePlan::getLhResultBatchNo, lhResultBatchNo));
        if (CollectionUtils.isEmpty(planList)) {
            return;
        }

        this.baseDao.updateBatch(this.resolveMouldCode(planList));
    }

    /**
     * 合并计划原模具号和匹配模具号。
     *
     * <p>原模具号在前、匹配模具号在后，去除空值和重复值；共享模具计划保留原有换行展示格式。</p>
     *
     * @param originalMouldCode 原模具号
     * @param matchedMouldCode  匹配模具号
     * @param changeMouldType   换模类型
     * @return 合并后的模具号
     */
    private String mergeMouldCode(String originalMouldCode, String matchedMouldCode, String changeMouldType) {
        LinkedHashSet<String> mouldCodeSet = LhMouldCodeUtil.splitMouldCode(originalMouldCode);
        mouldCodeSet.addAll(LhMouldCodeUtil.splitMouldCode(matchedMouldCode));
        if (CollectionUtils.isEmpty(mouldCodeSet)) {
            return "";
        }
        if (MouldChangeTypeEnum.containsCode(changeMouldType, MouldChangeTypeEnum.TYPE_BLOCK.getCode())) {
            return LhMouldCodeUtil.joinMouldCode(mouldCodeSet);
        }
        return mouldCodeSet.stream().collect(Collectors.joining(",\n"));
    }

    /**
     * 构建指定计划对应的在机模具信息映射。
     *
     * @param planList     模具交替计划列表
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 机台前缀到在机信息列表的映射
     */
    private Map<String, List<LhMachineOnlineInfo>> buildOnlineInfoMap(List<LhMouldChangePlan> planList,
                                                                      String factoryCode,
                                                                      Date scheduleDate) {
        Map<String, List<LhMachineOnlineInfo>> onlineInfoMap = new HashMap<>();
        List<String> machinePrefixList = planList.stream()
                .map(LhMouldChangePlan::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .map(this::stripLeftRightSuffix)
                .distinct()
                .collect(Collectors.toList());
        Date onlineDateEnd = this.getMouldOnlineDateEnd(factoryCode, scheduleDate);
        if (CollectionUtils.isEmpty(machinePrefixList) || onlineDateEnd == null) {
            return onlineInfoMap;
        }

        LambdaQueryWrapper<LhMachineOnlineInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(factoryCode)) {
            wrapper.eq(LhMachineOnlineInfo::getFactoryCode, factoryCode);
        }
        wrapper.and(prefixWrapper -> {
            for (String machinePrefix : machinePrefixList) {
                prefixWrapper.or().likeRight(LhMachineOnlineInfo::getLhCode, machinePrefix);
            }
        });
        wrapper.isNotNull(LhMachineOnlineInfo::getOnlineDate)
                .le(LhMachineOnlineInfo::getOnlineDate, onlineDateEnd)
                .isNotNull(LhMachineOnlineInfo::getInMachineMouldCode)
                .ne(LhMachineOnlineInfo::getInMachineMouldCode, "");
        List<LhMachineOnlineInfo> onlineInfoList = lhMachineOnlineInfoMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(onlineInfoList)) {
            return onlineInfoMap;
        }

        onlineInfoList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getLhCode()))
                .filter(item -> item.getOnlineDate() != null && !item.getOnlineDate().after(onlineDateEnd))
                .sorted(Comparator.comparing(LhMachineOnlineInfo::getLhCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(LhMachineOnlineInfo::getOnlineDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LhMachineOnlineInfo::getUpdateTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LhMachineOnlineInfo::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(onlineInfo -> onlineInfoMap.computeIfAbsent(
                        this.stripLeftRightSuffix(onlineInfo.getLhCode()), key -> new ArrayList<>()).add(onlineInfo));
        return onlineInfoMap;
    }

    /**
     * 批量查询共享模具花纹配置并按物料描述分组。
     *
     * @param planList    模具交替计划列表
     * @param factoryCode 工厂编码
     * @return 后物料描述到共享模具配置列表的映射
     */
    private Map<String, List<LhSharedMouldPat>> buildSharedMouldPatMap(List<LhMouldChangePlan> planList,
                                                                       String factoryCode) {
        List<String> materialDescList = planList.stream()
                .map(LhMouldChangePlan::getAfterMaterialDesc)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(materialDescList)) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<LhSharedMouldPat> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(factoryCode)) {
            wrapper.eq(LhSharedMouldPat::getFactoryCode, factoryCode);
        }
        wrapper.in(LhSharedMouldPat::getMaterialDesc, materialDescList);
        List<LhSharedMouldPat> sharedMouldPatList = lhSharedMouldPatEntityMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(sharedMouldPatList)) {
            return new HashMap<>();
        }
        return sharedMouldPatList.stream().collect(Collectors.groupingBy(LhSharedMouldPat::getMaterialDesc));
    }

    /**
     * 解析计划对应的在机模具号。
     *
     * @param plan          模具交替计划
     * @param onlineInfoMap 机台前缀到在机信息的映射
     * @return 去重后的在机模具号，多个模具号以英文逗号分隔
     */
    private String resolveOnlineMouldCode(LhMouldChangePlan plan,
                                          Map<String, List<LhMachineOnlineInfo>> onlineInfoMap) {
        String machinePrefix = this.stripLeftRightSuffix(plan.getLhMachineCode());
        List<LhMachineOnlineInfo> matchedInfoList = onlineInfoMap.get(machinePrefix);
        if (CollectionUtils.isEmpty(matchedInfoList)) {
            return "";
        }
        Map<String, LhMachineOnlineInfo> latestByMachine = new LinkedHashMap<>();
        matchedInfoList.forEach(onlineInfo -> latestByMachine.putIfAbsent(onlineInfo.getLhCode(), onlineInfo));
        return latestByMachine.values().stream()
                .map(LhMachineOnlineInfo::getInMachineMouldCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
    }

    /**
     * 解析计划对应的共享模具号和花纹块。
     *
     * @param plan              模具交替计划
     * @param sharedMouldPatMap 共享模具配置映射
     * @return 共享模具展示值，多个配置以换行分隔
     */
    private String resolveSharedMouldCode(LhMouldChangePlan plan,
                                          Map<String, List<LhSharedMouldPat>> sharedMouldPatMap) {
        List<LhSharedMouldPat> sharedMouldPatList = sharedMouldPatMap.get(plan.getAfterMaterialDesc());
        if (CollectionUtils.isEmpty(sharedMouldPatList)) {
            return "";
        }
        return sharedMouldPatList.stream()
                .map(sharedMouldPat -> StringUtils.defaultIfBlank(sharedMouldPat.getMouldNo(), "") + "/"
                        + StringUtils.defaultIfBlank(sharedMouldPat.getPatternBlock(), ""))
                .collect(Collectors.joining(",\n"));
    }

    /**
     * 获取模具交替计划模具号追溯天数。
     *
     * @param factoryCode 工厂编码
     * @return 追溯天数，参数不存在或无效时返回默认值2
     */
    private int getMouldChangePlanLookbackDays(String factoryCode) {
        int lookbackDays = 2;
        if (StringUtils.isBlank(factoryCode)) {
            return lookbackDays;
        }
        LhParams lookbackParam = lhParamsService.selectOneByParamCode(
                LhScheduleParamConstant.MOULD_CHANGE_PLAN_LOOKBACK_DAYS, factoryCode);
        if (lookbackParam == null || StringUtils.isBlank(lookbackParam.getParamValue())) {
            return lookbackDays;
        }
        try {
            return Integer.parseInt(lookbackParam.getParamValue().trim());
        } catch (NumberFormatException exception) {
            log.warn("模具交替计划模具号追溯天数参数无效，使用默认值2，工厂编码：{}", factoryCode);
            return lookbackDays;
        }
    }

    /**
     * 计算在机模具信息截止时间。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 在机信息截止时间，排程日期为空时返回null
     */
    private Date getMouldOnlineDateEnd(String factoryCode, Date scheduleDate) {
        if (scheduleDate == null) {
            return null;
        }
        return DateUtil.offsetDay(DateUtil.endOfDay(scheduleDate),
                -this.getMouldChangePlanLookbackDays(factoryCode));
    }

    /**
     * 去除机台编码末尾的左右模后缀。
     *
     * @param machineCode 机台编码
     * @return 去除后缀后的机台前缀
     */
    private String stripLeftRightSuffix(String machineCode) {
        if (StringUtils.isBlank(machineCode)) {
            return machineCode;
        }
        String normalizedMachineCode = machineCode.trim().toUpperCase();
        if (normalizedMachineCode.endsWith("L") || normalizedMachineCode.endsWith("R")) {
            return normalizedMachineCode.substring(0, normalizedMachineCode.length() - 1);
        }
        return normalizedMachineCode;
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }

    /**
     * 导入数据
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     * @return
     */
    @Override
    public AjaxResult importData(List<LhMouldChangePlan> list, boolean updateSupport, Long importLogId) {
        // 0.初始化
        int successNum = 0;
        int failureNum = 0;
        int insertNum = 0;
        int updateNum = 0;
        List<LhMouldChangePlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.unique");

        // 1.进行非空校验,Excel中数据重复校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhMouldChangePlan docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 2.进行数据库唯一性校验 + 硫化机台/物料存在性校验
        // 先批量查询，提升性能
        Map<String, List<LhMouldChangePlan>> factoryCodeMap = list.stream()
                .collect(Collectors.groupingBy(LhMouldChangePlan::getFactoryCode));

        // 查询硫化机台
        Map<String, LhMachineInfo> machineInfoMap = new HashMap<>(16);
        if (!factoryCodeMap.isEmpty()) {
            for (String factoryCode : factoryCodeMap.keySet()) {
                List<LhMouldChangePlan> itemList = factoryCodeMap.get(factoryCode);
                List<String> machineCodeList = itemList.stream()
                        .map(LhMouldChangePlan::getLhMachineCode)
                        .filter(StringUtil::isNotBlank)
                        .distinct()
                        .collect(Collectors.toList());
                if (!machineCodeList.isEmpty()) {
                    List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(machineCodeList, 900);
                    List<LhMachineInfo> machineInfoList = new ArrayList<>();
                    for (List<String> codeList : splitList) {
                        LambdaQueryWrapper<LhMachineInfo> wrapper = new LambdaQueryWrapper<>();
                        wrapper.in(LhMachineInfo::getMachineCode, codeList);
                        wrapper.eq(LhMachineInfo::getFactoryCode, factoryCode);
                        machineInfoList.addAll(lhMachineInfoEntityMapper.selectList(wrapper));
                    }
                    if (CollectionUtils.isNotEmpty(machineInfoList)) {
                        Map<String, LhMachineInfo> partMap = machineInfoList.stream().collect(Collectors
                                .toMap(x -> x.getFactoryCode() + "," + x.getMachineCode(), machine -> machine));
                        machineInfoMap.putAll(partMap);
                    }
                }
            }
        }

        // 查询物料 (前规格 + 后规格)
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>(16);
        if (!factoryCodeMap.isEmpty()) {
            for (String factoryCode : factoryCodeMap.keySet()) {
                List<LhMouldChangePlan> itemList = factoryCodeMap.get(factoryCode);
                List<String> materialCodeList = new ArrayList<>();
                for (LhMouldChangePlan item : itemList) {
                    if (StringUtil.isNotBlank(item.getBeforeMaterialCode())) {
                        materialCodeList.add(item.getBeforeMaterialCode());
                    }
                    if (StringUtil.isNotBlank(item.getAfterMaterialCode())) {
                        materialCodeList.add(item.getAfterMaterialCode());
                    }
                }
                materialCodeList = materialCodeList.stream().distinct().collect(Collectors.toList());
                if (!materialCodeList.isEmpty()) {
                    List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(materialCodeList, 900);
                    List<MdmMaterialInfo> materialInfoList = new ArrayList<>();
                    for (List<String> codeList : splitList) {
                        LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<>();
                        wrapper.in(MdmMaterialInfo::getMaterialCode, codeList);
                        wrapper.eq(MdmMaterialInfo::getFactoryCode, factoryCode);
                        materialInfoList.addAll(mdmMaterialInfoEntityMapper.selectList(wrapper));
                    }
                    if (CollectionUtils.isNotEmpty(materialInfoList)) {
                        Map<String, MdmMaterialInfo> partMap = materialInfoList.stream().collect(Collectors
                                .toMap(x -> x.getFactoryCode() + "," + x.getMaterialCode(), material -> material));
                        materialInfoMap.putAll(partMap);
                    }
                }
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhMouldChangePlan docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            // 检查硫化机台是否存在
            if (StringUtil.isBlank(docEntity.getLhMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.lhMachineCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, MessageFormat.format(message, errorNum), importErrorLogs);
                continue;
            }
            if (!machineInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getLhMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.lhMachineCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, MessageFormat.format(message, errorNum, docEntity.getLhMachineCode()), importErrorLogs);
                continue;
            }

            // 检查前规格物料是否存在
            if (StringUtil.isNotBlank(docEntity.getBeforeMaterialCode()) &&
                    !materialInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getBeforeMaterialCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.beforeMaterialCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, MessageFormat.format(message, errorNum, docEntity.getBeforeMaterialCode()), importErrorLogs);
                continue;
            }

            // 检查后规格物料是否存在
            if (StringUtil.isNotBlank(docEntity.getAfterMaterialCode()) &&
                    !materialInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getAfterMaterialCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.afterMaterialCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, MessageFormat.format(message, errorNum, docEntity.getAfterMaterialCode()), importErrorLogs);
                continue;
            }

            // 填充机台名称和物料描述
            if (machineInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getLhMachineCode())) {
                LhMachineInfo machine = machineInfoMap.get(docEntity.getFactoryCode() + "," + docEntity.getLhMachineCode());
                docEntity.setLhMachineName(machine.getMachineName());
            }
            if (StringUtil.isNotBlank(docEntity.getBeforeMaterialCode()) &&
                    materialInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getBeforeMaterialCode())) {
                MdmMaterialInfo material = materialInfoMap.get(docEntity.getFactoryCode() + "," + docEntity.getBeforeMaterialCode());
                docEntity.setBeforeMaterialDesc(material.getMaterialDesc());
            }
            if (StringUtil.isNotBlank(docEntity.getAfterMaterialCode()) &&
                    materialInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getAfterMaterialCode())) {
                MdmMaterialInfo material = materialInfoMap.get(docEntity.getFactoryCode() + "," + docEntity.getAfterMaterialCode());
                docEntity.setAfterMaterialDesc(material.getMaterialDesc());
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                docEntity.setRowState(RowStateEnum.ADDED);
                if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                    docEntity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                }
                Date orderDate = docEntity.getScheduleDate() != null ? docEntity.getScheduleDate() : new Date();
                docEntity.setOrderNo(orderNoGenerator.generateMouldChangeOrderNo(orderDate));
                docEntity.setIsRelease(ApsConstant.NO_RELEASE);
                docEntity.setMouldStatus(ApsConstant.FALSE);
                importList.add(docEntity);
            } else if (updateSupport) {
                LambdaQueryWrapper<LhMouldChangePlan> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(LhMouldChangePlan::getFactoryCode, docEntity.getFactoryCode());
                queryWrapper.eq(LhMouldChangePlan::getLhMachineCode, docEntity.getLhMachineCode());
                Date planDate = DateUtil.beginOfDay(docEntity.getPlanDate());
                queryWrapper.ge(LhMouldChangePlan::getPlanDate, planDate);
                queryWrapper.lt(LhMouldChangePlan::getPlanDate, DateUtil.offsetDay(planDate, 1));
                queryWrapper.eq(LhMouldChangePlan::getScheduleDate, docEntity.getScheduleDate());
                queryWrapper.eq(LhMouldChangePlan::getAfterMaterialCode, docEntity.getAfterMaterialCode());
                LhMouldChangePlan exist = lhMouldChangePlanMapper.selectOne(queryWrapper);
                if (exist == null) {
                    failureNum++;
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                            MessageFormat.format(uniqueMsg, errorNum), importErrorLogs);
                    continue;
                }
                docEntity.setId(exist.getId());
                docEntity.setOrderNo(exist.getOrderNo());
                docEntity.setIsRelease(exist.getIsRelease());
                docEntity.setMouldStatus(exist.getMouldStatus());
                lhMouldChangePlanMapper.updateById(docEntity);
                updateNum++;
            } else {
                failureNum++;
                // 数据库已经存在,不允许插入
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        MessageFormat.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        if (CollectionUtils.isEmpty(importList) && updateNum == 0) {
            String message = StringUtils.format(I18nUtil.getMessage("ui.message.import.fail"), successNum, failureNum);
            return AjaxResult.error(message, importErrorLogs);
        }

        if (CollectionUtils.isNotEmpty(importList)) {
            insertNum = baseDao.saveBatch(importList);
        }
        successNum = insertNum + updateNum;

        // 返回提示信息及错误集合
        if (failureNum > 0) {
            String message = StringUtils.format(I18nUtil.getMessage("ui.message.import.fail"), successNum, failureNum);
            return AjaxResult.error(message, importErrorLogs);
        } else {
            String message = StringUtils.format(I18nUtil.getMessage("ui.message.import.success"), successNum);
            return AjaxResult.success(message);
        }
    }

    /**
     * 校验唯一性
     */
    @Override
    public String checkUnique(LhMouldChangePlan docEntityVO) {
        // 唯一性判断维度：工厂 + 机台编码 + 计划日期 + 排程日期
        LambdaQueryWrapper<LhMouldChangePlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntityVO.getFieldValueByFieldName("id")),
                LhMouldChangePlan::getId, docEntityVO.getFieldValueByFieldName("id"));
        queryWrapper.eq(LhMouldChangePlan::getFactoryCode, docEntityVO.getFactoryCode());
        queryWrapper.eq(LhMouldChangePlan::getLhMachineCode, docEntityVO.getLhMachineCode());
        Date planDate = DateUtil.beginOfDay(docEntityVO.getPlanDate());
        queryWrapper.ge(LhMouldChangePlan::getPlanDate, planDate);
        queryWrapper.lt(LhMouldChangePlan::getPlanDate, DateUtil.offsetDay(planDate, 1));
        queryWrapper.eq(LhMouldChangePlan::getScheduleDate, docEntityVO.getScheduleDate());

        if (lhMouldChangePlanMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "lhMachineCode", "planDate", "scheduleDate");
    }

    /**
     * 排程发布
     * 校验规则：
     * 1. 未勾选记录时返回提示
     * 2. 勾选记录中包含历史记录（排程日期早于当前日期）时返回明确提示
     * 3. 已发布的数据不允许重复发布
     * 4. 班次为空的数据不允许发布
     */
    @Override
    public AjaxResult issueSchedule(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.noSelection"));
        }

        Long[] idArray = ids.toArray(new Long[0]);
        String lockKey = "lhMouldChangePlan:issue:lock" + Arrays.toString(idArray);
        if (redisService.getCacheObject(lockKey)!=null) {
            return AjaxResult.success();
        }
        try {
            redisService.setCacheObject(lockKey,"1");
            // 查询选中记录
            QueryWrapper<LhMouldChangePlan> wrapper = new QueryWrapper<>();
            wrapper.in("ID", ids);
            List<LhMouldChangePlan> planList = lhMouldChangePlanMapper.selectList(wrapper);
            if (CollectionUtils.isEmpty(planList)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.noData"));
            }

            // 校验是否包含历史记录（排程日期早于当前日期）
            Date today = DateUtil.beginOfDay(new Date());
            List<LhMouldChangePlan> historyList = planList.stream()
                    .filter(item -> item.getScheduleDate() != null && item.getScheduleDate().before(today))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(historyList)) {
                String historyDetails = historyList.stream()
                        .map(item -> String.format("%s/%s",
                                StringUtil.isNotBlank(item.getLhResultBatchNo()) ? item.getLhResultBatchNo() : "-",
                                StringUtil.isNotBlank(item.getOrderNo()) ? item.getOrderNo() : "-"))
                        .collect(Collectors.joining("; "));
                String msg = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.hasHistoryData");
                msg = StringUtils.format(msg, historyDetails);
                return AjaxResult.error(msg);
            }

            // 校验是否存在已发布的数据
            List<LhMouldChangePlan> releasedList = planList.stream()
                    .filter(item -> ApsConstant.IS_RELEASE.equals(item.getIsRelease()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(releasedList)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.hasReleasedData"));
            }

            // 校验班次是否为空，班次为空不允许发布
            List<LhMouldChangePlan> noClassIndexList = planList.stream()
                    .filter(item -> StringUtils.isBlank(item.getClassIndex()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(noClassIndexList)) {
                String noClassIndexDetails = noClassIndexList.stream()
                        .map(item -> String.format("%s/%s",
                                StringUtil.isNotBlank(item.getLhMachineCode()) ? item.getLhMachineCode() : "-",
                                StringUtil.isNotBlank(item.getOrderNo()) ? item.getOrderNo() : "-"))
                        .collect(Collectors.joining("; "));
                String msg = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.classIndexRequired");
                msg = StringUtils.format(msg, noClassIndexDetails);
                return AjaxResult.error(msg);
            }

            // 转换为MdmMoldAlterPlan
            List<MdmMoldAlterPlan> moldAlterPlanList = new ArrayList<>();
            for (LhMouldChangePlan plan : planList) {
                MdmMoldAlterPlan moldAlterPlan = new MdmMoldAlterPlan();
                BeanUtils.copyProperties(plan, moldAlterPlan);
                moldAlterPlan.setLhBatchNo(plan.getLhResultBatchNo());
                moldAlterPlan.setLeftRightMold(plan.getLeftRightMould());
                moldAlterPlan.setMaterialCode(plan.getBeforeMaterialCode());
                moldAlterPlan.setSpecDesc(plan.getBeforeMaterialDesc());
                moldAlterPlan.setPlanMaterialCode(plan.getAfterMaterialCode());
                moldAlterPlan.setPlanSpecDesc(plan.getAfterMaterialDesc());
                moldAlterPlan.setChangeMoldType(plan.getChangeMouldType());
                moldAlterPlan.setMoldNo(plan.getMouldCode());
                moldAlterPlan.setScheduleDate(plan.getScheduleDate());
                moldAlterPlanList.add(moldAlterPlan);
            }

            // 调用MES接口下发
            try {
                AjaxResult result = mesItfService.issueMoldAlterPlan(moldAlterPlanList);
                if (AjaxResult.Type.ERROR.value() != Integer.parseInt(result.get(AjaxResult.CODE_TAG).toString())) {
                    // 更新发布状态为已发布
                    for (LhMouldChangePlan plan : planList) {
                        plan.setIsRelease(ApsConstant.IS_RELEASE);
                    }
                    this.baseDao.updateBatch(planList);
                    return AjaxResult.success(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.issueSuccess"));
                } else {
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.issueFail"));
                }
            } catch (Exception e) {
                log.error("排程发布失败", e);
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.issueFail"));
            }
        }catch (Exception e){
            redisService.deleteObject(lockKey);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.issueFail"));
        } finally {
            redisService.deleteObject(lockKey);
        }
    }
}
