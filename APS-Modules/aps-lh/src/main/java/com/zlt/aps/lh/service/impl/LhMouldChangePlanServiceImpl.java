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
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.entity.*;
import com.zlt.aps.lh.api.domain.vo.LhMouldChangePlanVo;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.component.OrderNoGenerator;
import com.zlt.aps.lh.mapper.LhMachineOnlineInfoMapper;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
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

    /**
     * 备注：按时间下机文案国际化键。
     */
    private static final String REMARK_BY_TIME_KEY = "ui.data.column.lhMouldChangePlan.remark.byTime";

    /**
     * 备注：模具号前缀国际化键。
     */
    private static final String REMARK_MOULD_CODE_KEY =
            "ui.data.column.lhMouldChangePlan.remark.mouldCode";

    /**
     * 备注：按余量收尾文案国际化键。
     */
    private static final String REMARK_BY_REMAINDER_KEY =
            "ui.data.column.lhMouldChangePlan.remark.byRemainder";

    /**
     * 备注：干冰清洗文案国际化键。
     */
    private static final String REMARK_DRY_ICE_KEY = "ui.data.column.lhMouldChangePlan.remark.dryIce";

    /**
     * 备注：喷砂清洗文案国际化键。
     */
    private static final String REMARK_SAND_BLAST_KEY = "ui.data.column.lhMouldChangePlan.remark.sandBlast";

    /**
     * 备注：换活字块文案国际化键。
     */
    private static final String REMARK_TYPE_BLOCK_KEY =
            "ui.data.column.lhMouldChangePlan.remark.typeBlock";

    /**
     * 备注：前日计划变化文案国际化键。
     */
    private static final String REMARK_SCHEDULE_CHANGE_KEY =
            "ui.data.column.lhMouldChangePlan.remark.scheduleChange";

    /**
     * 班次备注国际化键前缀。
     */
    private static final String REMARK_SHIFT_KEY_PREFIX = "ui.data.column.lhMouldChangePlan.remark.shift.";

    /**
     * 中文备注日期格式。
     */
    private static final String REMARK_CHINESE_DATE_PATTERN = "MM.dd";

    /**
     * 越南语备注日期格式。
     */
    private static final String REMARK_VIETNAMESE_DATE_PATTERN = "dd/MM";

    /**
     * 越南语资源区域，确保命中 vi_VN 资源文件。
     */
    private static final Locale REMARK_VIETNAMESE_LOCALE = new Locale("vi", "VN");

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanMapper;

    @Resource
    private LhScheduleResultMapper lhScheduleResultMapper;

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
     * 按默认业务规则排序模具交替计划。
     * <p>换模时间相同的记录，再按关联硫化批次、机台和计划日期对应班次的计划量降序比较；
     * 当前班次计划量相同后，仅比较紧接的下一班次，仍相同则沿用原有稳定排序字段。</p>
     *
     * @param planList 待排序的模具交替计划列表
     * @return 排序后的新列表
     */
    @Override
    public List<LhMouldChangePlan> sortByDefaultOrder(List<LhMouldChangePlan> planList) {
        if (CollectionUtils.isEmpty(planList)) {
            return planList == null ? Collections.emptyList() : new ArrayList<>(planList);
        }
        Map<String, Integer> planQuantityMap = this.loadPlanQuantityMap(planList);
        Comparator<LhMouldChangePlan> comparator = Comparator
                .comparingLong((LhMouldChangePlan plan) -> plan.getPlanDate() == null
                        ? Long.MAX_VALUE : DateUtil.beginOfDay(plan.getPlanDate()).getTime())
                .thenComparingInt(plan -> this.resolveMouldChangeTypeSortOrder(plan))
                .thenComparing(plan -> StringUtils.defaultIfBlank(plan.getClassIndex(), ""))
                .thenComparing(LhMouldChangePlan::getChangeTime,
                        Comparator.nullsLast(Date::compareTo))
                .thenComparing((LhMouldChangePlan plan) -> this.resolvePlanQuantity(plan, planQuantityMap),
                        Comparator.reverseOrder())
                .thenComparing((LhMouldChangePlan plan) -> this.resolveNextShiftQuantity(plan, planQuantityMap),
                        Comparator.reverseOrder())
                .thenComparing(plan -> StringUtils.defaultIfBlank(plan.getLhMachineCode(), ""))
                .thenComparing(LhMouldChangePlan::getPlanOrder,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(LhMouldChangePlan::getId,
                        Comparator.nullsLast(Long::compareTo));
        return planList.stream().sorted(comparator).collect(Collectors.toList());
    }

    /**
     * 按默认业务规则排序模具交替计划导出视图。
     *
     * @param planList 待排序的导出视图列表
     * @return 排序后的导出视图列表
     */
    @Override
    public List<LhMouldChangePlanVo> sortVoByDefaultOrder(List<LhMouldChangePlanVo> planList) {
        if (CollectionUtils.isEmpty(planList)) {
            return planList == null ? Collections.emptyList() : new ArrayList<>(planList);
        }
        List<LhMouldChangePlan> entityList = new ArrayList<>(planList.size());
        IdentityHashMap<LhMouldChangePlan, LhMouldChangePlanVo> sourceMap = new IdentityHashMap<>();
        for (LhMouldChangePlanVo source : planList) {
            LhMouldChangePlan target = new LhMouldChangePlan();
            BeanUtils.copyProperties(source, target);
            entityList.add(target);
            sourceMap.put(target, source);
        }
        return this.sortByDefaultOrder(entityList).stream()
                .map(sourceMap::get)
                .collect(Collectors.toList());
    }

    /**
     * 批量加载关联硫化排程结果，并汇总到工厂、批次、机台、日期、班次和物料维度。
     *
     * @param planList 模具交替计划列表
     * @return 计划量匹配键到计划量的映射
     */
    private Map<String, Integer> loadPlanQuantityMap(List<LhMouldChangePlan> planList) {
        Set<String> batchNoSet = planList.stream()
                .filter(Objects::nonNull)
                .map(LhMouldChangePlan::getLhResultBatchNo)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .collect(Collectors.toSet());
        Set<String> factoryCodeSet = planList.stream()
                .filter(Objects::nonNull)
                .map(LhMouldChangePlan::getFactoryCode)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .collect(Collectors.toSet());
        Set<String> machineCodeSet = planList.stream()
                .filter(Objects::nonNull)
                .map(LhMouldChangePlan::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(batchNoSet) || CollectionUtils.isEmpty(factoryCodeSet)
                || CollectionUtils.isEmpty(machineCodeSet) || lhScheduleResultMapper == null) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<LhScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(LhScheduleResult::getBatchNo, batchNoSet)
                .in(LhScheduleResult::getFactoryCode, factoryCodeSet)
                .in(LhScheduleResult::getLhMachineCode, machineCodeSet);
        List<LhScheduleResult> resultList = lhScheduleResultMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(resultList)) {
            return Collections.emptyMap();
        }

        Map<String, Integer> quantityMap = new HashMap<>();
        for (LhScheduleResult result : resultList) {
            if (result == null || StringUtils.isBlank(result.getMaterialCode())) {
                continue;
            }
            for (int classNumber = 1; classNumber <= 8; classNumber++) {
                Date shiftStartTime = (Date) result.getFieldValueByFieldName(
                        String.format("class%dStartTime", classNumber));
                String shiftCode = this.resolveShiftCode(shiftStartTime);
                if (shiftStartTime == null || StringUtils.isBlank(shiftCode)) {
                    continue;
                }
                Object quantityValue = result.getFieldValueByFieldName(
                        String.format("class%dPlanQty", classNumber));
                int quantity = quantityValue instanceof Number ? ((Number) quantityValue).intValue() : 0;
                String key = this.buildPlanQuantityKey(result.getFactoryCode(), result.getBatchNo(),
                        result.getLhMachineCode(), result.getMaterialCode(), shiftStartTime, shiftCode);
                quantityMap.merge(key, quantity, Integer::sum);
            }
        }
        return quantityMap;
    }

    /**
     * 获取计划当前班次计划量。
     *
     * @param plan        模具交替计划
     * @param quantityMap 计划量映射
     * @return 当前班次计划量
     */
    private int resolvePlanQuantity(LhMouldChangePlan plan, Map<String, Integer> quantityMap) {
        if (plan == null || plan.getPlanDate() == null || StringUtils.isBlank(plan.getClassIndex())
                || StringUtils.isBlank(plan.getLhResultBatchNo())
                || StringUtils.isBlank(plan.getAfterMaterialCode())) {
            return 0;
        }
        String shiftCode = this.normalizeShiftCode(plan.getClassIndex());
        if (StringUtils.isBlank(shiftCode)) {
            return 0;
        }
        String key = this.buildPlanQuantityKey(plan.getFactoryCode(), plan.getLhResultBatchNo(),
                plan.getLhMachineCode(), plan.getAfterMaterialCode(), plan.getPlanDate(), shiftCode);
        return quantityMap.getOrDefault(key, 0);
    }

    /**
     * 获取计划紧接下一班的计划量。
     *
     * @param plan        模具交替计划
     * @param quantityMap 计划量映射
     * @return 下一班计划量
     */
    private int resolveNextShiftQuantity(LhMouldChangePlan plan, Map<String, Integer> quantityMap) {
        if (plan == null || plan.getPlanDate() == null || StringUtils.isBlank(plan.getClassIndex())
                || StringUtils.isBlank(plan.getLhResultBatchNo())
                || StringUtils.isBlank(plan.getAfterMaterialCode())) {
            return 0;
        }
        String currentShiftCode = this.normalizeShiftCode(plan.getClassIndex());
        if (StringUtils.isBlank(currentShiftCode)) {
            return 0;
        }
        String nextShiftCode;
        Date nextShiftDate = plan.getPlanDate();
        if (ShiftEnum.NIGHT_SHIFT.getCode().equals(currentShiftCode)) {
            nextShiftCode = ShiftEnum.MORNING_SHIFT.getCode();
            // 夜班从当天22:00跨到次日06:00，下一班按实际时间顺序落在次日。
            nextShiftDate = DateUtil.offsetDay(nextShiftDate, 1);
        } else if (ShiftEnum.MORNING_SHIFT.getCode().equals(currentShiftCode)) {
            nextShiftCode = ShiftEnum.AFTERNOON_SHIFT.getCode();
        } else {
            nextShiftCode = ShiftEnum.NIGHT_SHIFT.getCode();
        }
        String key = this.buildPlanQuantityKey(plan.getFactoryCode(), plan.getLhResultBatchNo(),
                plan.getLhMachineCode(), plan.getAfterMaterialCode(), nextShiftDate, nextShiftCode);
        return quantityMap.getOrDefault(key, 0);
    }

    /**
     * 构建计划量匹配键。
     *
     * @param factoryCode 工厂编码
     * @param batchNo     关联硫化批次
     * @param machineCode 硫化机台
     * @param materialCode 物料编码
     * @param shiftDate   班次日期
     * @param shiftCode   班次编码
     * @return 计划量匹配键
     */
    private String buildPlanQuantityKey(String factoryCode, String batchNo, String machineCode,
                                        String materialCode, Date shiftDate, String shiftCode) {
        return StringUtils.defaultString(factoryCode).trim() + "|"
                + StringUtils.defaultString(batchNo).trim() + "|"
                + StringUtils.defaultString(machineCode).trim() + "|"
                + StringUtils.defaultString(materialCode).trim() + "|"
                + (shiftDate == null ? "" : DateUtil.formatDate(DateUtil.beginOfDay(shiftDate))) + "|"
                + StringUtils.defaultString(shiftCode).trim();
    }

    /**
     * 将排程结果班次开始时间转换为业务班次编码。
     *
     * @param shiftStartTime 班次开始时间
     * @return 班次编码；无法识别时返回空
     */
    private String resolveShiftCode(Date shiftStartTime) {
        if (shiftStartTime == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(shiftStartTime);
        int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        if (minutes >= 22 * 60 || minutes < 6 * 60) {
            return ShiftEnum.NIGHT_SHIFT.getCode();
        }
        if (minutes < 14 * 60) {
            return ShiftEnum.MORNING_SHIFT.getCode();
        }
        return ShiftEnum.AFTERNOON_SHIFT.getCode();
    }

    /**
     * 规范模具计划班次编码。
     *
     * @param classIndex 原班次编码
     * @return 两位班次编码；无效时返回空
     */
    private String normalizeShiftCode(String classIndex) {
        String normalizedClassIndex = StringUtils.trim(classIndex);
        if (StringUtils.isBlank(normalizedClassIndex)) {
            return null;
        }
        ShiftEnum shiftEnum = ShiftEnum.getByCode(normalizedClassIndex);
        if (shiftEnum == null && normalizedClassIndex.length() == 1) {
            shiftEnum = ShiftEnum.getByCode("0" + normalizedClassIndex);
        }
        return shiftEnum == null ? null : shiftEnum.getCode();
    }

    /**
     * 解析模具交替类型默认排序优先级。
     *
     * @param plan 模具交替计划
     * @return 清洗计划返回0，交替计划返回1，其他计划返回2
     */
    private int resolveMouldChangeTypeSortOrder(LhMouldChangePlan plan) {
        if (plan == null) {
            return 2;
        }
        String changeMouldType = plan.getChangeMouldType();
        if (MouldChangeTypeEnum.containsAnyCode(changeMouldType,
                MouldChangeTypeEnum.SAND_BLAST.getCode(), MouldChangeTypeEnum.DRY_ICE.getCode())) {
            return 0;
        }
        return MouldChangeTypeEnum.containsAnyCode(changeMouldType,
                MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode()) ? 1 : 2;
    }

    /**
     * 解析模具交替计划最终使用的模具号。
     *
     * <p>换活字块计划继续取当前物理机台左右侧的在机模具号，并按原值优先的顺序合并；
     * 正规换模计划从后物料对应的共享模具配置中排除在机模具号后，稳定随机选择最多两副
     * 模具并直接覆盖原模具号；喷砂清洗和干冰清洗计划不填写模具号，其他类型继续沿用共享
     * 模具配置和原有合并逻辑。</p>
     *
     * <p>按计划记录的分厂和排程日期分组匹配来源数据，最终结果回填至原列表，供自动排程
     * 保存和模具号刷新使用；模具号解析完成后同步按业务规则重建备注。</p>
     *
     * @param planList 模具交替计划列表
     * @return 回填最终模具号后的模具交替计划列表
     */
    @Override
    public List<LhMouldChangePlan> resolveMouldCode(List<LhMouldChangePlan> planList) {
        if (CollectionUtils.isEmpty(planList)) {
            return planList == null ? Collections.emptyList() : planList;
        }

        Map<String, Map<String, List<LhMouldChangePlan>>> previousPlanMap =
                this.buildPreviousPlanMap(planList);

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
                    String changeMouldType = plan.getChangeMouldType();
                    if (MouldChangeTypeEnum.containsCode(changeMouldType,
                            MouldChangeTypeEnum.TYPE_BLOCK.getCode())) {
                        String matchedMouldCode = this.resolveOnlineMouldCode(plan, onlineInfoMap);
                        plan.setMouldCode(this.mergeMouldCode(plan.getMouldCode(), matchedMouldCode,
                                changeMouldType));
                    } else if (MouldChangeTypeEnum.containsCode(changeMouldType,
                            MouldChangeTypeEnum.REGULAR.getCode())) {
                        plan.setMouldCode(this.resolveRegularMouldCode(plan, onlineInfoMap, sharedMouldPatMap));
                    } else if (MouldChangeTypeEnum.containsCode(changeMouldType,
                            MouldChangeTypeEnum.SAND_BLAST.getCode())
                            || MouldChangeTypeEnum.containsCode(changeMouldType,
                            MouldChangeTypeEnum.DRY_ICE.getCode())) {
                        // 喷砂、干冰属于清洗计划，按业务口径不保存模具号。
                        plan.setMouldCode("");
                    } else {
                        String matchedMouldCode = this.resolveSharedMouldCode(plan, sharedMouldPatMap);
                        plan.setMouldCode(this.mergeMouldCode(plan.getMouldCode(), matchedMouldCode,
                                changeMouldType));
                    }
                    String scheduleGroupKey = this.buildScheduleGroupKey(plan.getFactoryCode(), plan.getScheduleDate());
                    plan.setRemark(this.buildMouldChangeRemark(plan,
                            previousPlanMap.getOrDefault(scheduleGroupKey, Collections.emptyMap())));
                }
            }
        }
        return planList;
    }

    /**
     * 批量查询每个当前排程日前一天的模具交替计划，并按业务匹配键归集。
     *
     * @param planList 当前待解析的模具交替计划
     * @return 排程分组键到前日业务匹配键及计划列表的映射
     */
    private Map<String, Map<String, List<LhMouldChangePlan>>> buildPreviousPlanMap(
            List<LhMouldChangePlan> planList) {
        Map<String, List<LhMouldChangePlan>> currentGroupMap = planList.stream()
                .filter(Objects::nonNull)
                .filter(plan -> plan.getScheduleDate() != null)
                .collect(Collectors.groupingBy(plan -> this.buildScheduleGroupKey(
                                plan.getFactoryCode(), plan.getScheduleDate()),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, Map<String, List<LhMouldChangePlan>>> previousPlanMap = new HashMap<>();
        for (Map.Entry<String, List<LhMouldChangePlan>> entry : currentGroupMap.entrySet()) {
            LhMouldChangePlan currentPlan = entry.getValue().get(0);
            Date currentScheduleDate = DateUtil.beginOfDay(currentPlan.getScheduleDate());
            Date previousDateStart = DateUtil.offsetDay(
                    DateUtil.beginOfDay(currentPlan.getScheduleDate()), -1);
            Date previousDateEnd = DateUtil.beginOfDay(currentPlan.getScheduleDate());
            LambdaQueryWrapper<LhMouldChangePlan> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.isBlank(currentPlan.getFactoryCode())) {
                wrapper.and(factoryWrapper -> factoryWrapper
                        .isNull(LhMouldChangePlan::getFactoryCode)
                        .or()
                        .eq(LhMouldChangePlan::getFactoryCode, ""));
            } else {
                wrapper.eq(LhMouldChangePlan::getFactoryCode, currentPlan.getFactoryCode());
            }
            wrapper.ge(LhMouldChangePlan::getScheduleDate, previousDateStart)
                    .lt(LhMouldChangePlan::getScheduleDate, previousDateEnd);
            List<LhMouldChangePlan> previousPlans = lhMouldChangePlanMapper.selectList(wrapper);
            Map<String, List<LhMouldChangePlan>> previousByBusinessKey = CollectionUtils.isEmpty(previousPlans)
                    ? Collections.emptyMap()
                    : previousPlans.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(this::buildMouldChangeBusinessKey,
                            LinkedHashMap::new, Collectors.toList()));
            previousPlanMap.put(entry.getKey(), previousByBusinessKey);
        }
        return previousPlanMap;
    }

    /**
     * 根据类型、下机方式、模具号和前日计划差异重建备注。
     * 按余量收尾文案仅在 END_TYPE=0 且 CHANGE_MOULD_TYPE 包含 01（正规换模）时生成；
     * 按时间下机文案仅在 END_TYPE=1 且 CHANGE_MOULD_TYPE 包含 01（正规换模）时生成。
     *
     * @param plan              已完成模具号解析的当前计划
     * @param previousPlanByKey 前日计划业务匹配键映射
     * @return 按固定顺序拼接的备注
     */
    private String buildMouldChangeRemark(LhMouldChangePlan plan,
                                          Map<String, List<LhMouldChangePlan>> previousPlanByKey) {
        List<String> remarkParts = new ArrayList<>();
        // 只有正规换模（01）且下机类型为按余量收尾（0）时，才追加按余量收尾文案。
        if (YesOrNoEnum.NO.getCode().equals(plan.getEndType())
                && MouldChangeTypeEnum.containsCode(plan.getChangeMouldType(),
                MouldChangeTypeEnum.REGULAR.getCode())) {
            remarkParts.add(I18nUtil.getMessage(REMARK_BY_REMAINDER_KEY));
        // 只有正规换模（01）且下机类型为按时间（1）时，才追加按时间下机文案。
        } else if (YesOrNoEnum.YES.getCode().equals(plan.getEndType())
                && MouldChangeTypeEnum.containsCode(plan.getChangeMouldType(),
                MouldChangeTypeEnum.REGULAR.getCode())) {
            remarkParts.add(I18nUtil.getMessage(REMARK_BY_TIME_KEY));
        }
        if (MouldChangeTypeEnum.containsCode(plan.getChangeMouldType(),
                MouldChangeTypeEnum.DRY_ICE.getCode())) {
            remarkParts.add(I18nUtil.getMessage(REMARK_DRY_ICE_KEY));
        }
        if (MouldChangeTypeEnum.containsCode(plan.getChangeMouldType(),
                MouldChangeTypeEnum.SAND_BLAST.getCode())) {
            remarkParts.add(I18nUtil.getMessage(REMARK_SAND_BLAST_KEY));
        }
        if (MouldChangeTypeEnum.containsCode(plan.getChangeMouldType(),
                MouldChangeTypeEnum.TYPE_BLOCK.getCode())) {
            remarkParts.add(I18nUtil.getMessage(REMARK_TYPE_BLOCK_KEY));
        }
        if (StringUtils.isNotBlank(plan.getMouldCode())) {
            remarkParts.add(I18nUtil.getMessage(REMARK_MOULD_CODE_KEY) + plan.getMouldCode());
        }
        String scheduleChangeRemark = this.resolveScheduleChangeRemark(plan, previousPlanByKey);
        if (StringUtils.isNotBlank(scheduleChangeRemark)) {
            remarkParts.add(scheduleChangeRemark);
        }
        return String.join("；", remarkParts);
    }

    /**
     * 判断当前计划相对前日计划是否发生日期或班次变化。
     *
     * @param currentPlan       当前计划
     * @param previousPlanByKey 前日计划业务匹配键映射
     * @return 前日计划变化说明；无法唯一判断时返回空字符串
     */
    private String resolveScheduleChangeRemark(LhMouldChangePlan currentPlan,
                                               Map<String, List<LhMouldChangePlan>> previousPlanByKey) {
        if (currentPlan.getPlanDate() == null || StringUtils.isBlank(currentPlan.getClassIndex())
                || previousPlanByKey == null) {
            return "";
        }
        List<LhMouldChangePlan> candidates = previousPlanByKey.get(this.buildMouldChangeBusinessKey(currentPlan));
        if (CollectionUtils.isEmpty(candidates)) {
            return "";
        }
        if (candidates.stream().anyMatch(previousPlan -> this.isSamePlanPosition(currentPlan, previousPlan))) {
            return "";
        }
        if (candidates.stream().anyMatch(previousPlan -> previousPlan.getPlanDate() == null
                || StringUtils.isBlank(previousPlan.getClassIndex()))) {
            return "";
        }
        Map<String, LhMouldChangePlan> distinctCandidates = candidates.stream()
                .filter(previousPlan -> previousPlan.getPlanDate() != null
                        && StringUtils.isNotBlank(previousPlan.getClassIndex()))
                .collect(Collectors.toMap(this::buildPlanPositionKey, previousPlan -> previousPlan,
                        (firstPlan, secondPlan) -> firstPlan, LinkedHashMap::new));
        if (distinctCandidates.size() != 1) {
            if (distinctCandidates.size() > 1) {
                log.debug("前日模具交替计划存在多个不同日期班次，跳过计划变化说明，业务键：{}",
                        this.buildMouldChangeBusinessKey(currentPlan));
            }
            return "";
        }
        LhMouldChangePlan previousPlan = distinctCandidates.values().iterator().next();
        int positionCompare = this.comparePlanPosition(currentPlan, previousPlan);
        if (positionCompare == 0) {
            return "";
        }
        String previousChineseDate = DateUtil.format(previousPlan.getPlanDate(), REMARK_CHINESE_DATE_PATTERN);
        String currentChineseDate = DateUtil.format(currentPlan.getPlanDate(), REMARK_CHINESE_DATE_PATTERN);
        String previousVietnameseDate = DateUtil.format(previousPlan.getPlanDate(),
                REMARK_VIETNAMESE_DATE_PATTERN);
        String currentVietnameseDate = DateUtil.format(currentPlan.getPlanDate(),
                REMARK_VIETNAMESE_DATE_PATTERN);
        String previousChineseShift = this.resolveShiftDisplayName(previousPlan.getClassIndex(),
                Locale.SIMPLIFIED_CHINESE);
        String currentChineseShift = this.resolveShiftDisplayName(currentPlan.getClassIndex(),
                Locale.SIMPLIFIED_CHINESE);
        String previousVietnameseShift = this.resolveShiftDisplayName(previousPlan.getClassIndex(),
                REMARK_VIETNAMESE_LOCALE);
        String currentVietnameseShift = this.resolveShiftDisplayName(currentPlan.getClassIndex(),
                REMARK_VIETNAMESE_LOCALE);
        return MessageFormat.format(I18nUtil.getMessage(REMARK_SCHEDULE_CHANGE_KEY),
                previousChineseDate, previousChineseShift, currentChineseDate, currentChineseShift,
                previousVietnameseDate, previousVietnameseShift, currentVietnameseDate, currentVietnameseShift);
    }

    /**
     * 比较两个计划的实际执行先后，日期优先，同日再比较实际时间和班次开始时间。
     *
     * @param currentPlan  当前计划
     * @param previousPlan 前日计划
     * @return 当前计划晚于前日计划返回正数，早于返回负数，无法判断返回0
     */
    private int comparePlanPosition(LhMouldChangePlan currentPlan, LhMouldChangePlan previousPlan) {
        int dateCompare = DateUtil.beginOfDay(currentPlan.getPlanDate())
                .compareTo(DateUtil.beginOfDay(previousPlan.getPlanDate()));
        if (dateCompare != 0) {
            return dateCompare;
        }
        Date currentExecutionTime = this.resolveExecutionTime(currentPlan);
        Date previousExecutionTime = this.resolveExecutionTime(previousPlan);
        if (currentExecutionTime != null && previousExecutionTime != null) {
            int timeCompare = currentExecutionTime.compareTo(previousExecutionTime);
            if (timeCompare != 0) {
                return timeCompare;
            }
        }
        int currentShiftMinutes = this.resolveShiftStartMinutes(currentPlan.getClassIndex());
        int previousShiftMinutes = this.resolveShiftStartMinutes(previousPlan.getClassIndex());
        if (currentShiftMinutes < 0 || previousShiftMinutes < 0) {
            return 0;
        }
        return Integer.compare(currentShiftMinutes, previousShiftMinutes);
    }

    /**
     * 解析计划实际执行时间，优先使用换模时间，缺失时回退到计划时间。
     *
     * @param plan 模具交替计划
     * @return 实际执行时间
     */
    private Date resolveExecutionTime(LhMouldChangePlan plan) {
        return plan.getChangeTime() != null ? plan.getChangeTime() : plan.getPlanDate();
    }

    /**
     * 判断两个计划是否日期和班次均未变化。
     *
     * @param currentPlan  当前计划
     * @param previousPlan 前日计划
     * @return 日期和班次均相同返回true
     */
    private boolean isSamePlanPosition(LhMouldChangePlan currentPlan, LhMouldChangePlan previousPlan) {
        return previousPlan != null && previousPlan.getPlanDate() != null
                && DateUtil.beginOfDay(currentPlan.getPlanDate()).equals(DateUtil.beginOfDay(previousPlan.getPlanDate()))
                && StringUtils.equals(currentPlan.getClassIndex().trim(), previousPlan.getClassIndex().trim());
    }

    /**
     * 构建排程日期分组键。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 分组键
     */
    private String buildScheduleGroupKey(String factoryCode, Date scheduleDate) {
        return StringUtils.defaultIfBlank(factoryCode, "") + "|"
                + (scheduleDate == null ? "" : DateUtil.formatDate(scheduleDate));
    }

    /**
     * 构建机台、前物料和后物料组成的业务匹配键。
     *
     * @param plan 模具交替计划
     * @return 业务匹配键
     */
    private String buildMouldChangeBusinessKey(LhMouldChangePlan plan) {
        return String.join("|", StringUtils.defaultIfBlank(plan.getLhMachineCode(), ""),
                StringUtils.defaultIfBlank(plan.getBeforeMaterialCode(), ""),
                StringUtils.defaultIfBlank(plan.getAfterMaterialCode(), ""));
    }

    /**
     * 构建日期和班次组成的位置键。
     *
     * @param plan 模具交替计划
     * @return 位置键
     */
    private String buildPlanPositionKey(LhMouldChangePlan plan) {
        return DateUtil.formatDate(plan.getPlanDate()) + "|" + plan.getClassIndex().trim();
    }

    /**
     * 解析指定语言的班次展示名称。
     *
     * @param classIndex 班次编码
     * @param locale     班次展示语言
     * @return 指定语言的班次名称，未知编码返回原值
     */
    private String resolveShiftDisplayName(String classIndex, Locale locale) {
        ShiftEnum shiftEnum = this.resolveShiftEnum(classIndex);
        if (shiftEnum == null) {
            return StringUtils.defaultIfBlank(classIndex, "");
        }
        String shiftDisplayName = I18nUtil.getMessage(REMARK_SHIFT_KEY_PREFIX + shiftEnum.getCode(), locale);
        return StringUtils.isBlank(shiftDisplayName) ? shiftEnum.getDescription() : shiftDisplayName;
    }

    /**
     * 解析班次枚举，兼容一位数字班次编码。
     *
     * @param classIndex 班次编码
     * @return 班次枚举，未知编码返回null
     */
    private ShiftEnum resolveShiftEnum(String classIndex) {
        String normalizedClassIndex = StringUtils.trim(classIndex);
        if (normalizedClassIndex == null) {
            return null;
        }
        ShiftEnum shiftEnum = ShiftEnum.getByCode(normalizedClassIndex);
        if (shiftEnum == null && normalizedClassIndex.length() == 1) {
            shiftEnum = ShiftEnum.getByCode("0" + normalizedClassIndex);
        }
        return shiftEnum;
    }

    /**
     * 获取班次开始时间的分钟数。
     *
     * @param classIndex 班次编码
     * @return 开始时间分钟数，未知班次返回-1
     */
    private int resolveShiftStartMinutes(String classIndex) {
        ShiftEnum shiftEnum = this.resolveShiftEnum(classIndex);
        if (shiftEnum == null || StringUtils.isBlank(shiftEnum.getStartTime())) {
            return -1;
        }
        String[] timeParts = shiftEnum.getStartTime().split(":");
        if (timeParts.length != 2) {
            return -1;
        }
        try {
            return Integer.parseInt(timeParts[0]) * 60 + Integer.parseInt(timeParts[1]);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    /**
     * 解析正规换模计划的模具号。
     *
     * <p>候选模具按模具号去重并稳定排序后，使用计划记录自身生成的随机种子打乱顺序，
     * 最多选择两副。没有候选模具时返回空字符串，避免原模具号或在机模具号重新进入结果。</p>
     *
     * @param plan              模具交替计划
     * @param onlineInfoMap     机台前缀到在机信息的映射
     * @param sharedMouldPatMap 后物料描述到共享模具配置的映射
     * @return 排除在机模具后的随机模具号和花纹块，多个配置以换行分隔
     */
    private String resolveRegularMouldCode(LhMouldChangePlan plan,
                                           Map<String, List<LhMachineOnlineInfo>> onlineInfoMap,
                                           Map<String, List<LhSharedMouldPat>> sharedMouldPatMap) {
        List<LhSharedMouldPat> sharedMouldPatList = sharedMouldPatMap.get(plan.getAfterMaterialDesc());
        if (CollectionUtils.isEmpty(sharedMouldPatList)) {
            return "";
        }

        Set<String> inMachineMouldCodeSet = LhMouldCodeUtil.splitMouldCode(
                this.resolveOnlineMouldCode(plan, onlineInfoMap));
        Map<String, LhSharedMouldPat> mouldCandidateMap = sharedMouldPatList.stream()
                .filter(Objects::nonNull)
                .filter(sharedMouldPat -> StringUtils.isNotBlank(sharedMouldPat.getMouldNo()))
                .filter(sharedMouldPat -> !inMachineMouldCodeSet.contains(sharedMouldPat.getMouldNo().trim()))
                .sorted(Comparator.comparing((LhSharedMouldPat sharedMouldPat) ->
                                sharedMouldPat.getMouldNo().trim())
                        .thenComparing(sharedMouldPat -> StringUtils.defaultIfBlank(
                                sharedMouldPat.getPatternBlock(), ""))
                        .thenComparing(LhSharedMouldPat::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toMap(sharedMouldPat -> sharedMouldPat.getMouldNo().trim(),
                        sharedMouldPat -> sharedMouldPat,
                        (firstMould, secondMould) -> firstMould,
                        LinkedHashMap::new));
        if (mouldCandidateMap.isEmpty()) {
            return "";
        }

        List<LhSharedMouldPat> candidateList = new ArrayList<>(mouldCandidateMap.values());
        Collections.shuffle(candidateList, new Random(this.resolveRegularMouldRandomSeed(plan)));
        return candidateList.stream()
                .limit(2)
                .map(sharedMouldPat -> sharedMouldPat.getMouldNo().trim() + "/"
                        + StringUtils.defaultIfBlank(sharedMouldPat.getPatternBlock(), ""))
                .collect(Collectors.joining(",\n"));
    }

    /**
     * 生成正规换模计划的稳定随机种子。
     *
     * <p>优先使用计划主键；主键为空时使用计划业务字段生成稳定键，确保同一计划重复解析时
     * 的随机结果一致，不受数据库返回顺序影响。</p>
     *
     * @param plan 模具交替计划
     * @return 稳定随机种子
     */
    private long resolveRegularMouldRandomSeed(LhMouldChangePlan plan) {
        if (plan.getId() != null) {
            return plan.getId();
        }
        String businessKey = String.join("|",
                StringUtils.defaultIfBlank(plan.getFactoryCode(), ""),
                plan.getScheduleDate() == null ? "" : String.valueOf(plan.getScheduleDate().getTime()),
                plan.getPlanDate() == null ? "" : String.valueOf(plan.getPlanDate().getTime()),
                StringUtils.defaultIfBlank(plan.getLhMachineCode(), ""),
                StringUtils.defaultIfBlank(plan.getLeftRightMould(), ""),
                StringUtils.defaultIfBlank(plan.getBeforeMaterialCode(), ""),
                StringUtils.defaultIfBlank(plan.getAfterMaterialCode(), ""));
        return businessKey.hashCode();
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
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    this.getCheckUniqueFields().toArray(new String[0]));
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
                LhMouldChangePlan exist = lhMouldChangePlanMapper.selectOne(this.buildUniqueQueryWrapper(docEntity));
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
        // 唯一性判断维度：工厂 + 排程日期 + 计划日期 + 机台编码 + 左右模
        if (lhMouldChangePlanMapper.selectCount(this.buildUniqueQueryWrapper(docEntityVO)) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    /**
     * 构建模具交替计划导入和手工维护共用的唯一键查询条件。
     * 计划日期按自然日范围匹配，排程日期沿用日期字段的精确匹配口径。
     *
     * @param docEntity 待校验或待更新的模具交替计划
     * @return 按工厂、排程日期、计划日期、机台和左右模组成的查询条件
     */
    private LambdaQueryWrapper<LhMouldChangePlan> buildUniqueQueryWrapper(LhMouldChangePlan docEntity) {
        LambdaQueryWrapper<LhMouldChangePlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntity.getFieldValueByFieldName("id")),
                LhMouldChangePlan::getId, docEntity.getFieldValueByFieldName("id"));
        queryWrapper.eq(LhMouldChangePlan::getFactoryCode, docEntity.getFactoryCode());
        queryWrapper.eq(LhMouldChangePlan::getScheduleDate, docEntity.getScheduleDate());
        Date planDate = DateUtil.beginOfDay(docEntity.getPlanDate());
        queryWrapper.ge(LhMouldChangePlan::getPlanDate, planDate);
        queryWrapper.lt(LhMouldChangePlan::getPlanDate, DateUtil.offsetDay(planDate, 1));
        queryWrapper.eq(LhMouldChangePlan::getLhMachineCode, docEntity.getLhMachineCode());
        if (docEntity.getLeftRightMould() == null) {
            queryWrapper.isNull(LhMouldChangePlan::getLeftRightMould);
        } else {
            queryWrapper.eq(LhMouldChangePlan::getLeftRightMould, docEntity.getLeftRightMould());
        }
        return queryWrapper;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "scheduleDate", "planDate", "lhMachineCode", "leftRightMould");
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
