package com.zlt.aps.dj.service.impl;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.enums.HalfComponentFinishTableEnum;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.domain.ScheduleSummaryVo;
import com.zlt.aps.common.engine.enums.ClassNumThreePlanEnums;
import com.zlt.aps.common.engine.service.impl.BaseFinishQtyImportService;
import com.zlt.aps.dj.api.domain.entity.DjDayFinishQty;
import com.zlt.aps.dj.api.domain.entity.DjDispatcherLog;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.api.domain.entity.DjParams;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.aps.dj.api.domain.entity.DjShiftConfig;
import com.zlt.aps.dj.engine.constant.DjEngineConstants;
import com.zlt.aps.dj.engine.service.impl.DjEngineNewServiceImpl;
import com.zlt.aps.dj.engine.vo.DjScheduleResultVo;
import com.zlt.aps.dj.mapper.DjScheduleResultMapper;
import com.zlt.aps.dj.mapper.DjParamsMapper;
import com.zlt.aps.dj.service.DjDispatcherLogService;
import com.zlt.aps.dj.service.DjMachineInfoService;
import com.zlt.aps.dj.service.DjScheduleResultService;
import com.zlt.aps.dj.service.IDjScheduleAdjustService;
import com.zlt.aps.dj.service.IDjShiftConfigService;
import com.zlt.aps.utils.BillUtils;
import com.zlt.bill.common.service.AbstractBillService;

import lombok.extern.slf4j.Slf4j;

/**
 * 垫胶胶排程结果Service业务层处理
 *
 * @author zlt
 * @date 2026-06-24
 */
@Slf4j
@Service
public class DjScheduleResultServiceImpl extends AbstractBillService<DjScheduleResult>
        implements DjScheduleResultService {
    @Resource
    private DjScheduleResultMapper djScheduleResultMapper;

    @Autowired
    private DjMachineInfoService machineInfoService;

    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;

    @Resource
    private DjDispatcherLogService djDispatcherLogService;

    @Autowired
    private DjParamsMapper djParamsMapper;

    @Autowired
    private IDjScheduleAdjustService iDjScheduleAdjustService;

    @Autowired
    private IDjShiftConfigService djShiftConfigService;

    /**
     * 查询垫胶排程结果
     *
     * @param id 垫胶排程结果ID
     * @return 垫胶排程结果
     */
    @Override
    public DjScheduleResult selectDjScheduleResultById(Long id) {
        return djScheduleResultMapper.selectById(id);
    }

    /**
     * 查询垫胶排程结果列表
     *
     * @param djScheduleResult 垫胶排程结果
     * @return 垫胶排程结果
     */
    @Override
    public List<DjScheduleResult> selectDjScheduleResultList(DjScheduleResult djScheduleResult) {
        QueryWrapper<DjScheduleResult> queryWrapper = BillUtils.builderCondition(djScheduleResult);
        List<DjScheduleResult> list = djScheduleResultMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        // 加载机台名称
        this.fillMachineName(list);
        // 加载 T-1 日早班计划量（class3）
        this.fillPrevDayClass3Plan(list, djScheduleResult.getScheduleDate());
        return list;
    }

    /**
     * 填充机台名称
     */
    @Override
    public void fillMachineName(List<DjScheduleResult> list) {
        List<DjMachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(new DjMachineInfo());
        Map<String, DjMachineInfo> machineInfoMap = machineInfoList.stream()
                .collect(Collectors.toMap(DjMachineInfo::getMachineCode, Function.identity(), (s1, s2) -> s1));
        for (DjScheduleResult scheduleResult : list) {
            String machineCodeStr = scheduleResult.getMachineCode();
            if (StringUtils.isNotBlank(machineCodeStr)) {
                List<String> machineNameList = new ArrayList<>();
                String[] machineIdArr = machineCodeStr.split(",");
                for (String machineCode : machineIdArr) {
                    String key = machineCode;
                    if (machineInfoMap.containsKey(key)) {
                        DjMachineInfo machineInfo = machineInfoMap.get(key);
                        machineNameList.add(machineInfo.getMachineName());
                    }
                }
                scheduleResult.setMachineName(String.join(",", machineNameList));
            }
        }
    }

    /**
     * 填充 T-1 日数据（按排程首班班次动态加载对应的早班/中班字段）
     * <p>
     * 排程首班班次决定需要加载哪些 T-1 日班次数据：
     * <ul>
     *   <li>首班=中班("03")：加载 T-1 日早班（class3）数据 → prevDayClass3*</li>
     *   <li>首班=夜班("01")：加载 T-1 日早班 + 中班数据 → prevDayClass3* + prevDayClass1*</li>
     *   <li>首班=早班("02")：无需加载 T-1 日数据</li>
     * </ul>
     * 首班班次优先取 T 日已有排产结果的 scheduleShiftClass，无数据时从 DjShiftConfig 中获取 crossDayFlag="1" 的班次。
     * T-1 日具体加载的 classX 字段由其自身的 scheduleShiftClass 动态映射。
     */
    @Override
    public void fillPrevDayClass3Plan(List<DjScheduleResult> list, Date scheduleDate) {
        if (scheduleDate == null || CollectionUtils.isEmpty(list)) {
            return;
        }

        // 1. 确定排程首班班次
        String startShiftClass = this.getStartShiftClass(list);
        if (startShiftClass == null) {
            startShiftClass = ClassNumThreePlanEnums.CLASS_DAY.getClassIndex(); // 默认中班
        }

        // 首班=早班时，不需要加载 T-1 日数据
        if (ClassNumThreePlanEnums.CLASS_MORNING.getClassIndex().equals(startShiftClass)) {
            return;
        }

        // 首班=夜班时，需要加载早班+中班；首班=中班时，只需要加载早班
        boolean needClass1 = ClassNumThreePlanEnums.CLASS_NIGHT.getClassIndex().equals(startShiftClass); // 是否需要中班数据

        // 2. 加载 T-1 日排产结果
        Date prevDate = DateUtils.addDays(scheduleDate, -1);
        List<DjScheduleResult> prevDayList = djScheduleResultMapper.selectList(
                new LambdaQueryWrapper<DjScheduleResult>()
                        .eq(DjScheduleResult::getScheduleDate, prevDate));
        if (CollectionUtils.isEmpty(prevDayList)) {
            return;
        }

        // 3. 确定 T-1 日各字段映射：根据 T-1 日结果的 scheduleShiftClass 判断
        //    如 T-1 结果中有且已设置 scheduleShiftClass，取第一条；否则直接用 T 日的首班班次
        String prevScheduleShiftClass = null;
        for (DjScheduleResult r : prevDayList) {
            if (r.getScheduleShiftClass() != null) {
                prevScheduleShiftClass = r.getScheduleShiftClass();
                break;
            }
        }
        if (prevScheduleShiftClass == null) {
            prevScheduleShiftClass = startShiftClass; // 与 T 日一致
        }

        // 早班在 T-1 日对应的 classX
        int earlyClassIndex = this.realShiftToClassIndex(prevScheduleShiftClass,
                ClassNumThreePlanEnums.CLASS_MORNING.getClassIndex());
        // 中班在 T-1 日对应的 classX
        int middleClassIndex = this.realShiftToClassIndex(prevScheduleShiftClass,
                ClassNumThreePlanEnums.CLASS_DAY.getClassIndex());

        // 4. 按 machineCode + paddingCode 汇总 T-1 日数据
        Map<String, DjScheduleResult> prevDayEarlyFirstMap = new HashMap<>();
        Map<String, BigDecimal> prevDayEarlyPlanSumMap = new HashMap<>();
        Map<String, DjScheduleResult> prevDayMiddleFirstMap = new HashMap<>();
        Map<String, BigDecimal> prevDayMiddlePlanSumMap = new HashMap<>();

        for (DjScheduleResult prev : prevDayList) {
            String key = prev.getMachineCode() + ":" + prev.getPaddingCode();

            // 早班数据
            DjScheduleResult earlyFirst = prevDayEarlyFirstMap.get(key);
            if (earlyFirst == null) {
                // 创建临时对象仅保存早班字段值
                DjScheduleResult wrap = new DjScheduleResult();
                wrap.setClass3Sequence(
                        (Integer) prev.getFieldValueByFieldName(String.format("class%dSequence", earlyClassIndex)));
                Object earlyPlanQty = prev.getFieldValueByFieldName(String.format("class%dPlanQty", earlyClassIndex));
                wrap.setClass3PlanQty(BigDecimalUtils.valueOf(earlyPlanQty));
                wrap.setClass3FinishQty((BigDecimal) prev
                        .getFieldValueByFieldName(String.format("class%dFinishQty", earlyClassIndex)));
                wrap.setClass3FinishRate((BigDecimal) prev
                        .getFieldValueByFieldName(String.format("class%dFinishRate", earlyClassIndex)));
                wrap.setClass3Analysis((String) prev
                        .getFieldValueByFieldName(String.format("class%dAnalysis", earlyClassIndex)));
                prevDayEarlyFirstMap.put(key, wrap);
            }
            BigDecimal planQty = BigDecimalUtils.valueOf(
                    prev.getFieldValueByFieldName(String.format("class%dPlanQty", earlyClassIndex)));
            if (planQty.compareTo(BigDecimal.ZERO) > 0) {
                prevDayEarlyPlanSumMap.merge(key, planQty, BigDecimal::add);
            }

            // 中班数据（仅首班=夜班时需要）
            if (needClass1) {
                DjScheduleResult middleFirst = prevDayMiddleFirstMap.get(key);
                if (middleFirst == null) {
                    DjScheduleResult wrap = new DjScheduleResult();
                    wrap.setClass1Sequence(
                            (Integer) prev.getFieldValueByFieldName(String.format("class%dSequence", middleClassIndex)));
                    Object middlePlanQty = prev.getFieldValueByFieldName(
                            String.format("class%dPlanQty", middleClassIndex));
                    wrap.setClass1PlanQty(BigDecimalUtils.valueOf(middlePlanQty));
                    wrap.setClass1FinishQty((BigDecimal) prev
                            .getFieldValueByFieldName(String.format("class%dFinishQty", middleClassIndex)));
                    wrap.setClass1FinishRate((BigDecimal) prev
                            .getFieldValueByFieldName(String.format("class%dFinishRate", middleClassIndex)));
                    wrap.setClass1Analysis((String) prev
                            .getFieldValueByFieldName(String.format("class%dAnalysis", middleClassIndex)));
                    prevDayMiddleFirstMap.put(key, wrap);
                }
                BigDecimal middlePlanQty = BigDecimalUtils.valueOf(
                        prev.getFieldValueByFieldName(String.format("class%dPlanQty", middleClassIndex)));
                if (middlePlanQty.compareTo(BigDecimal.ZERO) > 0) {
                    prevDayMiddlePlanSumMap.merge(key, middlePlanQty, BigDecimal::add);
                }
            }
        }

        // 5. 填充到当前结果集
        for (DjScheduleResult curr : list) {
            String key = curr.getMachineCode() + ":" + curr.getPaddingCode();

            // 填充早班数据 → prevDayClass3*
            DjScheduleResult earlyFirst = prevDayEarlyFirstMap.get(key);
            if (earlyFirst != null) {
                curr.setPrevDayClass3Sequence(earlyFirst.getClass3Sequence());
                curr.setPrevDayClass3FinishQty(earlyFirst.getClass3FinishQty());
                curr.setPrevDayClass3FinishRate(earlyFirst.getClass3FinishRate());
                curr.setPrevDayClass3Analysis(earlyFirst.getClass3Analysis());
            }
            BigDecimal earlyPlanSum = prevDayEarlyPlanSumMap.get(key);
            if (earlyPlanSum != null) {
                curr.setPrevDayClass3PlanQty(earlyPlanSum);
            }

            // 填充中班数据 → prevDayClass1*
            if (needClass1) {
                DjScheduleResult middleFirst = prevDayMiddleFirstMap.get(key);
                if (middleFirst != null) {
                    curr.setPrevDayClass1Sequence(middleFirst.getClass1Sequence());
                    curr.setPrevDayClass1FinishQty(middleFirst.getClass1FinishQty());
                    curr.setPrevDayClass1FinishRate(middleFirst.getClass1FinishRate());
                    curr.setPrevDayClass1Analysis(middleFirst.getClass1Analysis());
                }
                BigDecimal middlePlanSum = prevDayMiddlePlanSumMap.get(key);
                if (middlePlanSum != null) {
                    curr.setPrevDayClass1PlanQty(middlePlanSum);
                }
            }
        }
    }

    /**
     * 获取排程首班班次
     * <p>优先取当前排产结果的 scheduleShiftClass，无数据时从 DjShiftConfig 中获取 crossDayFlag="1" 的班次。</p>
     */
    private String getStartShiftClass(List<DjScheduleResult> list) {
        // 先看 T 日是否已有排产结果
        for (DjScheduleResult r : list) {
            if (r.getScheduleShiftClass() != null) {
                return r.getScheduleShiftClass();
            }
        }
        // 从班次配置中获取跨天班次（crossDayFlag="1"）作为首班班次
        try {
            List<DjShiftConfig> activeShifts = djShiftConfigService.listActiveShifts();
            for (DjShiftConfig shift : activeShifts) {
                if (ApsConstant.TRUE.equals(shift.getCrossDayFlag())) {
                    return shift.getShiftCode();
                }
            }
        } catch (Exception e) {
            // 查询失败时使用默认值
            log.error(e.getMessage(), e);
        }
        return ClassNumThreePlanEnums.CLASS_NIGHT.getClassIndex();
    }

    /**
     * 将真实班次映射为 T-1 日排产结果的 classX 索引
     * <p>
     * 根据 T-1 日的 scheduleShiftClass 确定 class1~class3 对应的真实班次：
     * <ul>
     *   <li>scheduleShiftClass="03"(中班) → class1=中班, class2=夜班, class3=早班</li>
     *   <li>scheduleShiftClass="01"(夜班) → class1=夜班, class2=早班, class3=中班</li>
     *   <li>scheduleShiftClass="02"(早班) → class1=早班, class2=中班, class3=夜班</li>
     * </ul>
     *
     * @param scheduleShiftClass T-1 日排程首班班次
     * @param realShiftClass     真实班次（01=夜班, 02=早班, 03=中班）
     * @return 对应的 classX 索引（1~3）
     */
    private int realShiftToClassIndex(String scheduleShiftClass, String realShiftClass) {
        ClassNumThreePlanEnums current = ClassNumThreePlanEnums.getClassEnums(scheduleShiftClass);
        if (current == null) {
            current = ClassNumThreePlanEnums.CLASS_DAY;
        }
        for (int i = 0; i < 3; i++) {
            if (current.getClassIndex().equals(realShiftClass)) {
                return i + 1;
            }
            current = current.getNextClass();
        }
        return 3; // 默认返回 class3
    }

    /**
     * 新增垫胶排程结果
     *
     * @param djScheduleResult 垫胶排程结果
     * @return 结果
     */
    @Override
    public int insertDjScheduleResult(DjScheduleResult djScheduleResult) {
        djScheduleResult.setBaseVale(null);
        DjScheduleResultVo scheduleVo = new DjScheduleResultVo();
        BeanUtils.copyProperties(djScheduleResult, scheduleVo);
//        return djEngineService.insertDjOrder(scheduleVo);
        return 0;
    }

    /**
     * 修改垫胶排程结果
     *
     * @param scheduleResult 垫胶排程结果
     * @return 结果
     */
    @Override
    public int updateDjScheduleResult(DjScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        // 校验字段是否修改，修改则改状态为未发布
        if (!ApsConstant.RELEASING.equals(scheduleResult.getReleaseStatus())
                || !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getReleaseStatus())
                || StringUtils.isEmpty(scheduleResult.getReleaseStatus())) {
            DjScheduleResult scheduleResult2 = djScheduleResultMapper.selectById(scheduleResult.getId());
            boolean flag = compare(scheduleResult2.getMachineCode(), scheduleResult.getMachineCode());
            flag = flag && Objects.compare(scheduleResult2.getClass1PlanQty(), scheduleResult.getClass1PlanQty(),
                    BigDecimal::compareTo) != 0;
            flag = flag && Objects.compare(scheduleResult2.getClass2PlanQty(), scheduleResult.getClass2PlanQty(),
                    BigDecimal::compareTo) != 0;
            flag = flag && Objects.compare(scheduleResult2.getClass3PlanQty(), scheduleResult.getClass3PlanQty(),
                    BigDecimal::compareTo) != 0;
            flag = flag && compare(scheduleResult2.getClass1Analysis(), scheduleResult.getClass1Analysis());
            flag = flag && compare(scheduleResult2.getClass2Analysis(), scheduleResult.getClass2Analysis());
            flag = flag && compare(scheduleResult2.getClass3Analysis(), scheduleResult.getClass1Analysis());
            flag = flag && compare(scheduleResult2.getRemark(), scheduleResult.getRemark());
            if (!flag) {
                scheduleResult.setReleaseStatus(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE
                        : ApsConstant.WAIT_RELEASING);
            }
        }
        LambdaUpdateWrapper<DjScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(DjScheduleResult::getUpdateBy, scheduleResult.getUpdateBy());
        updateWrapper.set(DjScheduleResult::getUpdateTime, scheduleResult.getUpdateTime());
        updateWrapper.set(DjScheduleResult::getReleaseStatus, scheduleResult.getReleaseStatus());
        updateWrapper.eq(DjScheduleResult::getId, scheduleResult.getId());
        return djScheduleResultMapper.update(scheduleResult, updateWrapper);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * 
     * @param operType    操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    @Override
    public void insertDispatcherLog(String operType, DjScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        // if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        // return;
        // }
        DjScheduleResult oldSchedule = this.djScheduleResultMapper.selectById(newSchedule.getId()); // 操作前的排程数据
        // 构建日志并保存
        djDispatcherLogService.saveBill(this.buildDispatcherLog(operType, newSchedule, oldSchedule));
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insertDispatcherLogInsertOrder(String operType, List<DjScheduleResult> scheduleResults,
            DjScheduleResult newSchedule) {
        List<DjScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        // 基础信息赋值
        newSchedule.setId(scheduleResultList.get(0).getId());
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        DjScheduleResult oldSchedule = null;
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            oldSchedule = scheduleResults.stream().max(Comparator.comparing(DjScheduleResult::getCreateTime))
                    .orElse(null);
        }
        // 构建日志并保存
        djDispatcherLogService.saveBill(this.buildDispatcherLog(operType, newSchedule, oldSchedule));
    }

    /*
     * 构建排产操作操作日志
     */
    private DjDispatcherLog buildDispatcherLog(String operType, DjScheduleResult newSchedule,
            DjScheduleResult oldSchedule) {
        DjDispatcherLog log = new DjDispatcherLog();
        // 基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate()); // 排程日期
        log.setMaterialCode(newSchedule.getPaddingCode()); // 垫胶代码
        // 操作前的信息赋值
        if (oldSchedule != null) {
            log.setBeforeMachineCode(oldSchedule.getMachineCode());
            log.setBeforeClass1PlanQty(oldSchedule.getClass1PlanQty());
            log.setBeforeClass2PlanQty(oldSchedule.getClass2PlanQty());
            log.setBeforeClass3PlanQty(oldSchedule.getClass3PlanQty());
        }
        // 操作后的信息赋值
        log.setBeforeMachineCode(newSchedule.getMachineCode());
        log.setBeforeClass1PlanQty(newSchedule.getClass1PlanQty());
        log.setBeforeClass2PlanQty(newSchedule.getClass2PlanQty());
        log.setBeforeClass3PlanQty(newSchedule.getClass3PlanQty());
        return log;
    }

    /**
     * 根据排程日期和代码查询排程结果
     * 
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    @Override
    public List<DjScheduleResult> selectByScheduleDateAndCode(DjScheduleResult scheduleResult) {
        return djScheduleResultMapper.selectList(BillUtils.builderCondition(scheduleResult));
    }

    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    public boolean compare(Double d1, Double d2) {
        d1 = ObjectUtils.isEmpty(d1) ? 0D : d1;
        d2 = ObjectUtils.isEmpty(d2) ? 0D : d2;
        return d1.equals(d2);
    }

    public boolean compare(Long l1, Long l2) {
        return (l1 == null ? l2 == null : l1.equals(l2));
    }

    /**
     * 批量删除垫胶排程结果
     *
     * @param ids 需要删除的垫胶排程结果ID
     * @return 结果
     */
    @Override
    public int deleteDjScheduleResultByIds(Long[] ids) {
        return djScheduleResultMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 删除垫胶排程结果信息
     *
     * @param id 垫胶排程结果ID
     * @return 结果
     */
    @Override
    public int deleteDjScheduleResultById(Long id) {
        return djScheduleResultMapper.deleteById(id);
    }

    /**
     * 批量更新发布状态
     *
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdate(long[] ids, Date scheduleDate, String dataVersion, String factoryCode, String companyCode) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_DJ);
        record.setScheduleDate(scheduleDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        record.setDataVersion(dataVersion);
        this.deployDjScheduleToMid(ids, dataVersion, factoryCode, companyCode); // 把排程数据发布到中间库，并通知MES
//        djScheduleResultMapper.insert(record);
//        return djScheduleResultMapper.batchUpdate(Arrays.stream(ids).boxed().collect(Collectors.toList()),
//                ApsConstant.RELEASING);
        return 0;
    }

    /**
     * 更新指定相关数据记录的发布状态
     *
     * @param dataVersion 数据版本
     * @param ids         排程ID列表
     * @param status      更新的状态
     */
    @Override
    public void updateRelaseStatus(String dataVersion, long[] ids, String status) {
        LambdaUpdateWrapper<DjScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(DjScheduleResult::getReleaseStatus, status);
        updateWrapper.in(DjScheduleResult::getId, ids);
        djScheduleResultMapper.update(null, updateWrapper);
//        djScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 把排程数据发布到中间库
     *
     * @param ids 排程id
     */
    private void deployDjScheduleToMid(long[] ids, String dataVersion, String factoryCode, String companyCode) {
        if (ids == null) {
            return;
        }
        // TODO 调用itf接口
//        djScheduleResultMapper.deployDjScheduleToMid(dataVersion, ids, factoryCode, companyCode); // 把排程数据同步到接口中间库中
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @Override
    public Boolean isPublish(Date scheduleDate) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_DJ);
        record.setScheduleDate(scheduleDate);
//        return djScheduleResultMapper.isPublish(record) > 0;
        return true;
    }

    /**
     * 唯一性校验（校验该排产日期+机台+垫胶代码+工厂下是否存在排程记录）
     * <p>注意：只使用 factoryCode、scheduleDate、machineCode、paddingCode 作为查询条件，
     * 排除班次计划量、顺位等班次字段，避免因待插入记录不存在完全匹配而导致误判"该日未排程"。</p>
     */
    @Override
    public List<DjScheduleResult> checkUnique(DjScheduleResult entity) {
        Long id = entity.getId();
        QueryWrapper<DjScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", entity.getFactoryCode())
                .eq("SCHEDULE_DATE", entity.getScheduleDate())
                .eq("MACHINE_CODE", entity.getMachineCode())
                .eq("PADDING_CODE", entity.getPaddingCode());
        if (id != null) {
            queryWrapper.ne("id", id);
        }
        return djScheduleResultMapper.selectList(queryWrapper);
    }

    /**
     * 导入数据，并保存记录
     */
    @Override
    @Transactional
    public AjaxResult importData(List<DjScheduleResult> list, Long importLogId, String scheduleDate) {

        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<DjScheduleResult> importList = new ArrayList<>();
        DjMachineInfo djMachineInfo = new DjMachineInfo();
        djMachineInfo.setStatus("0");
        List<DjMachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(djMachineInfo);
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }

        // 根据机台名称去重
        TreeSet<DjMachineInfo> treeSet = new TreeSet<DjMachineInfo>(new Comparator<DjMachineInfo>() {
            @Override
            public int compare(DjMachineInfo o1, DjMachineInfo o2) {
                return o1.getMachineName().compareTo(o2.getMachineName());
            }
        });
        treeSet.addAll(machineInfoList);
        machineInfoList = new ArrayList<>(treeSet);

        Map<String, Long> machineCodeMap = machineInfoList.stream()
                .collect(Collectors.toMap(DjMachineInfo::getMachineName, DjMachineInfo::getId));
        // 按业务主键分组
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(a -> (a.getPaddingCode() + a.getMachineCode()), Collectors.counting()));

        // 遍历校验
        for (int i = 0; i < list.size(); i++) {
            DjScheduleResult entity = list.get(i);
            entity.setDataSource(DjEngineConstants.DATA_SOURCE_IMPORT);
            entity.setScheduleDate(DateUtils.dateTime("yyyy-MM-dd", scheduleDate));

            // 重复记录校验
            Long hasValue = groupMap.get(entity.getPaddingCode() + entity.getMachineCode());
            if (hasValue > 1) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.liningCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 3, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 3, entity);
            // 机台code 转为机台id
            if (entity.getMachineCode() != null && entity.getMachineCode().indexOf(",") > 0) {
                String message = I18nUtil.getMessage("ui.data.column.machine.produceLineValidate");
                message = String.format(message, i + 3,
                        I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine"));
                addImportErrorLog(importLogId, i + 3, message, validated);
            }
            if (machineCodeMap.get(entity.getMachineCode()) == null) {
                addImportErrorLog(importLogId, i + 3,
                        I18nUtil.getMessage("ui.error.message.column.produceLineNotExist"), validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                entity.setMachineCode(machineCodeMap.get(entity.getMachineCode()) + "");
                successNum++;
                entity.setBaseVale(null);
                importList.add(entity);
            }
        }
        this.batchSaveDjSchedule(scheduleDate, importList); // 把验证成功的记录进行导入

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 批量更新或新增排程记录信息
     *
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param importList   导入数据
     */
    private void batchSaveDjSchedule(String scheduleDate, List<DjScheduleResult> importList) {
        List<DjScheduleResultVo> scheduleList = new ArrayList<>();
        for (DjScheduleResult result : importList) {
            DjScheduleResultVo vo = new DjScheduleResultVo();
            BeanUtils.copyProperties(result, vo);
            scheduleList.add(vo);
        }
//        if (!scheduleList.isEmpty()) {
//            this.djEngineService.batchSaveDjSchedule(scheduleDate, scheduleList);
//        }
    }

    /**
     * 选机台
     */
    @Override
    public AjaxResult chooseMachine(DjScheduleResult scheduleResult) {
        if (CollectionUtils.isNotEmpty(this.checkUnique(scheduleResult))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
//        this.djEngineService.confirmDjMachine(scheduleResult); // 确认自动排程机台
        scheduleResult.setReleaseStatus(
                scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
//        djScheduleResultMapper.update(scheduleResult, updateWrapper);
        return AjaxResult.success();
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByDate(Date scheduleDate) {
//        return djScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
        return 0;
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
//        return djScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
        return 0;
    }

    /**
     * 更改发布状态
     *
     * @param entity 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(DjScheduleResult entity) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(1L);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_DJ);
        record.setScheduleDate(entity.getScheduleDate());
        record.setPublishStatus(entity.getReleaseStatus());
//        djScheduleResultMapper.updatePublishRecord(record);
//        return djScheduleResultMapper.changeReleaseStatus(entity);
        return 0;
    }

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     * @return 修改行数
     */
    @Override
    public int combinationMiddleAndNight(Long[] ids, String classifiedShift) {
        Map<String, Object> map = new HashMap<>();
        map.put("classifiedShift", classifiedShift);
        map.put("ids", ids);
//        return djScheduleResultMapper.combinationMiddleAndNight(map);
        return 0;
    }

    @Override
    public int checkDjCodeExist(DjScheduleResult djScheduleResult) {
//        return djScheduleResultMapper.checkDjCodeExist(djScheduleResult);
        return 0;
    }

    @Override
    public int isPublishByIds(Long[] ids) {
//        return djScheduleResultMapper.isPublishByIds(ids);
        return 0;
    }

    @Override
    public List<DjScheduleResult> selectByIds(List<Long> ids2) {
//        return djScheduleResultMapper.selectByIds(ids2);
        return null;
    }

    @Autowired
    private BaseFinishQtyImportService baseFinishQtyImportService;

    /**
     * 导入数据，并保存记录
     *
     * @param list        要导入数据
     * @param importLogId 导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importFinishQty(List<DjDayFinishQty> list, Long importLogId) {
//        return baseFinishQtyImportService.importFinishQty(list, importLogId, HalfComponentFinishTableEnum.DJ);
        return AjaxResult.error();
    }

    /**
     * 获取排程日期的排程结果合计。
     * <p>直接汇总 class1PlanQty / class2PlanQty / class3PlanQty，
     * 实体中 class1 即为排产起始班次，对应连续3个班。</p>
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @Override
    public AjaxResult getSummaryVo(DjScheduleResult scheduleResult) {
        List<DjScheduleResult> djScheduleResultList = this.selectDjScheduleResultList(scheduleResult);
        BigDecimal totalClass1PlanQty = BigDecimal.ZERO;
        BigDecimal totalClass2PlanQty = BigDecimal.ZERO;
        BigDecimal totalClass3PlanQty = BigDecimal.ZERO;
        BigDecimal totalStockQty = BigDecimal.ZERO;
        BigDecimal totalPrevDayClass3PlanQty = BigDecimal.ZERO;

        for (DjScheduleResult result : djScheduleResultList) {
            totalClass1PlanQty = totalClass1PlanQty.add(BigDecimalUtils.valueOf(result.getClass1PlanQty()));
            totalClass2PlanQty = totalClass2PlanQty.add(BigDecimalUtils.valueOf(result.getClass2PlanQty()));
            totalClass3PlanQty = totalClass3PlanQty.add(BigDecimalUtils.valueOf(result.getClass3PlanQty()));
            totalStockQty = totalStockQty.add(BigDecimalUtils.valueOf(result.getStockQty()));
            totalPrevDayClass3PlanQty = totalPrevDayClass3PlanQty.add(BigDecimalUtils.valueOf(result.getPrevDayClass3PlanQty()));
        }

        // 获取排产起始班次（shiftOrder最小的班次编码）
        String startShiftClass = ClassNumThreePlanEnums.CLASS_NIGHT.getClassIndex(); // 默认夜班
        List<DjShiftConfig> activeShifts = djShiftConfigService.listActiveShifts();
        if (CollectionUtils.isNotEmpty(activeShifts)) {
            startShiftClass = activeShifts.stream().filter(r -> ApsConstant.TRUE.equals(r.getOpenFlag())).findFirst()
                    .map(DjShiftConfig::getShiftCode).orElse(startShiftClass);
        }

        ScheduleSummaryVo scheduleSummaryVo = new ScheduleSummaryVo();
        scheduleSummaryVo.setClass1PlanQty(totalClass1PlanQty.doubleValue());
        scheduleSummaryVo.setClass2PlanQty(totalClass2PlanQty.doubleValue());
        scheduleSummaryVo.setClass3PlanQty(totalClass3PlanQty.doubleValue());
        scheduleSummaryVo.setStockQty(totalStockQty.doubleValue());
        scheduleSummaryVo.setLastDayPlanQty(totalPrevDayClass3PlanQty.doubleValue());
        scheduleSummaryVo.setScheduleShiftClass(startShiftClass);
        return AjaxResult.success(scheduleSummaryVo);
    }

    @Override
    public int importData(List<DjScheduleResult> list, boolean updateSupport, long importLogId) {
        return 0;
    }

    @Override
    protected String getBillTypeCode() {
        return "";
    }
}