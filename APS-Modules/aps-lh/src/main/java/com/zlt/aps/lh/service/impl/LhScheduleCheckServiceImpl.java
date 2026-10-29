package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.schedule.CxEmbryoEndingShift;
import com.zlt.aps.cx.util.LhVirtualMachineUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleCheckItemVO;
import com.zlt.aps.lh.api.domain.vo.LhScheduleCheckReportVO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.check.LhScheduleCheckContext;
import com.zlt.aps.lh.check.LhScheduleChecker;
import com.zlt.aps.lh.mapper.CxEmbryoEndingShiftMapper;
import com.zlt.aps.lh.mapper.LhScheduleCheckResultMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.mapper.MdmCapsuleChuckMapper;
import com.zlt.aps.lh.mapper.MdmMaterialInfoMapper;
import com.zlt.aps.lh.mapper.MdmModelInfoMapper;
import com.zlt.aps.lh.mapper.MdmMouldShellInfoMapper;
import com.zlt.aps.lh.mapper.MdmSkuMouldRelMapper;
import com.zlt.aps.lh.service.ILhScheduleCheckService;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmModelInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldShellInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.entity.MdmCapsuleChuck;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 硫化排程结果合理性校验服务实现。
 *
 * <p>校验键为「工厂编号 + 业务排程日」，批次号仅作追溯字段。
 * 每次执行先物理删除同工厂同业务日的旧记录，再全量重跑落库，保证同一数据多次
 * 校验得到同一结果（幂等）。</p>
 *
 * @author APS
 * @date 2026/10/27
 */
@Slf4j
@Service
public class LhScheduleCheckServiceImpl
        extends ServiceImpl<LhScheduleCheckResultMapper, LhScheduleCheckResult>
        implements ILhScheduleCheckService {

    /** 系统用户（无登录上下文时的确认人兜底） */
    private static final String SYSTEM_USER = "system";

    /** 成型收尾记录查询窗口：业务日往前天数 */
    private static final int CX_ENDING_WINDOW_BEFORE_DAYS = 3;

    /** 成型收尾记录查询窗口：业务日往后天数 */
    private static final int CX_ENDING_WINDOW_AFTER_DAYS = 4;

    /** 行唯一键分隔符 */
    private static final String ROW_KEY_SEPARATOR = "|";

    /**
     * 校验器集合（按 Spring 注册顺序执行，报告分组也按此顺序）
     */
    @Autowired
    private List<LhScheduleChecker> checkers;

    /** 排程结果 Mapper */
    @Resource
    private LhScheduleResultMapper lhScheduleResultMapper;

    /** 模具台账 Mapper */
    @Resource
    private MdmModelInfoMapper mdmModelInfoMapper;

    /** 模壳台账 Mapper */
    @Resource
    private MdmMouldShellInfoMapper mdmMouldShellInfoMapper;

    /** 胶囊卡盘台账 Mapper */
    @Resource
    private MdmCapsuleChuckMapper mdmCapsuleChuckMapper;

    /** SKU模具关系 Mapper */
    @Resource
    private MdmSkuMouldRelMapper mdmSkuMouldRelMapper;

    /** 物料主数据 Mapper */
    @Resource
    private MdmMaterialInfoMapper mdmMaterialInfoMapper;

    /** 成型收尾记录 Mapper */
    @Resource
    private CxEmbryoEndingShiftMapper cxEmbryoEndingShiftMapper;

    /**
     * 执行校验并落库（幂等替换），返回最新报告。
     *
     * <p>使用 REQUIRES_NEW 独立事务：排程/导入主流程即便后续回滚，
     * 校验结果也不受影响；反过来校验失败也不影响主流程。</p>
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（T 日）
     * @param triggerType 触发类型
     * @return 校验报告
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public LhScheduleCheckReportVO execute(String factoryCode, Date scheduleDate, String triggerType) {
        LhScheduleCheckContext context = this.buildContext(factoryCode, scheduleDate);
        Date checkTime = new Date();
        String batchNo = this.resolveBatchNo(context.getResults());
        List<LhScheduleCheckResult> allRecords = new ArrayList<>();
        for (LhScheduleChecker checker : checkers) {
            List<LhScheduleCheckResult> records;
            try {
                records = checker.check(context);
            } catch (Exception e) {
                // 单个检查项异常降级，不影响其他检查项
                log.error("硫化排程校验项执行异常, checkCode={}, factoryCode={}, scheduleDate={}",
                        checker.getCode(), factoryCode, DateUtil.formatDate(scheduleDate), e);
                continue;
            }
            if (records == null || records.isEmpty()) {
                continue;
            }
            for (LhScheduleCheckResult record : records) {
                record.setFactoryCode(factoryCode);
                record.setScheduleDate(scheduleDate);
                record.setCheckCode(checker.getCode());
                record.setCheckName(checker.getNameKey());
                record.setCheckLevel(checker.isHardControl()
                        ? LhScheduleCheckResult.CHECK_LEVEL_HARD : LhScheduleCheckResult.CHECK_LEVEL_TIP);
                record.setTriggerType(triggerType);
                record.setBatchNo(batchNo);
                record.setAcknowledged(LhScheduleCheckResult.ACKNOWLEDGED_NO);
                record.setCheckTime(checkTime);
            }
            allRecords.addAll(records);
        }
        // 先物理删旧再全量写入，保证同日重跑幂等
        this.baseMapper.physicalDeleteByFactoryAndDate(factoryCode, DateUtil.formatDate(scheduleDate));
        if (!allRecords.isEmpty()) {
            this.saveBatch(allRecords);
        }
        log.info("硫化排程校验完成, factoryCode={}, scheduleDate={}, triggerType={}, 异常记录数={}",
                factoryCode, DateUtil.formatDate(scheduleDate), triggerType, allRecords.size());
        return this.getReport(factoryCode, scheduleDate);
    }

    /**
     * 查询校验报告（不重新校验）。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（T 日）
     * @return 校验报告
     */
    @Override
    public LhScheduleCheckReportVO getReport(String factoryCode, Date scheduleDate) {
        LhScheduleCheckReportVO report = new LhScheduleCheckReportVO();
        report.setFactoryCode(factoryCode);
        report.setScheduleDate(scheduleDate == null ? null : DateUtil.formatDate(scheduleDate));
        List<LhScheduleCheckResult> records = this.listRecords(factoryCode, scheduleDate);
        Map<String, List<LhScheduleCheckResult>> recordsByCode = records.stream()
                .collect(Collectors.groupingBy(LhScheduleCheckResult::getCheckCode,
                        LinkedHashMap::new, Collectors.toList()));
        List<LhScheduleCheckItemVO> items = new ArrayList<>();
        boolean hasPending = false;
        boolean hasHardPending = false;
        for (LhScheduleChecker checker : checkers) {
            List<LhScheduleCheckResult> itemRecords = recordsByCode.getOrDefault(
                    checker.getCode(), new ArrayList<>());
            LhScheduleCheckItemVO item = new LhScheduleCheckItemVO();
            item.setCheckCode(checker.getCode());
            item.setCheckName(I18nUtil.getMessage(checker.getNameKey()));
            item.setCheckLevel(checker.isHardControl()
                    ? LhScheduleCheckResult.CHECK_LEVEL_HARD : LhScheduleCheckResult.CHECK_LEVEL_TIP);
            item.setRecords(itemRecords);
            int pendingCount = (int) itemRecords.stream()
                    .filter(record -> !LhScheduleCheckResult.ACKNOWLEDGED_YES.equals(record.getAcknowledged()))
                    .count();
            item.setPendingCount(pendingCount);
            item.setCheckTime(itemRecords.stream()
                    .map(LhScheduleCheckResult::getCheckTime)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .orElse(null));
            if (pendingCount > 0) {
                hasPending = true;
                if (checker.isHardControl()) {
                    hasHardPending = true;
                }
            }
            items.add(item);
        }
        report.setItems(items);
        report.setHasPending(hasPending);
        report.setHasHardPending(hasHardPending);
        Date maxCheckTime = records.stream()
                .map(LhScheduleCheckResult::getCheckTime)
                .filter(Objects::nonNull)
                .max(Date::compareTo)
                .orElse(null);
        report.setCheckTime(maxCheckTime);
        report.setDataStale(this.resolveDataStale(factoryCode, scheduleDate, maxCheckTime));
        return report;
    }

    /**
     * 判断是否存在未确认异常。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（T 日）
     * @return true-存在未确认异常
     */
    @Override
    public boolean hasPending(String factoryCode, Date scheduleDate) {
        LambdaQueryWrapper<LhScheduleCheckResult> wrapper = new LambdaQueryWrapper<LhScheduleCheckResult>()
                .eq(LhScheduleCheckResult::getFactoryCode, factoryCode)
                .eq(LhScheduleCheckResult::getScheduleDate, scheduleDate)
                .eq(LhScheduleCheckResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .eq(LhScheduleCheckResult::getAcknowledged, LhScheduleCheckResult.ACKNOWLEDGED_NO);
        return this.count(wrapper) > 0;
    }

    /**
     * 将某工厂某业务日的所有异常整体标记为已了解。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（T 日）
     */
    @Override
    public void acknowledge(String factoryCode, Date scheduleDate) {
        LambdaUpdateWrapper<LhScheduleCheckResult> wrapper = new LambdaUpdateWrapper<LhScheduleCheckResult>()
                .eq(LhScheduleCheckResult::getFactoryCode, factoryCode)
                .eq(LhScheduleCheckResult::getScheduleDate, scheduleDate)
                .eq(LhScheduleCheckResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .eq(LhScheduleCheckResult::getAcknowledged, LhScheduleCheckResult.ACKNOWLEDGED_NO)
                .set(LhScheduleCheckResult::getAcknowledged, LhScheduleCheckResult.ACKNOWLEDGED_YES)
                .set(LhScheduleCheckResult::getAcknowledgeBy, this.resolveCurrentUserName())
                .set(LhScheduleCheckResult::getAcknowledgeTime, new Date());
        this.update(wrapper);
    }

    /**
     * 构建校验上下文：加载结果集（剔除虚拟机台、跨批次去重）与各类台账数据。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（T 日）
     * @return 校验上下文
     */
    private LhScheduleCheckContext buildContext(String factoryCode, Date scheduleDate) {
        LhScheduleCheckContext context = new LhScheduleCheckContext();
        context.setFactoryCode(factoryCode);
        context.setScheduleDate(scheduleDate);
        // 1. 查询排程结果（该表无逻辑删除注解，显式过滤）
        List<LhScheduleResult> rawResults = lhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<LhScheduleResult>()
                        .eq(LhScheduleResult::getFactoryCode, factoryCode)
                        .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                        .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        if (rawResults.isEmpty()) {
            log.warn("硫化排程校验：未查询到排程结果, factoryCode={}, scheduleDate={}",
                    factoryCode, DateUtil.formatDate(scheduleDate));
        }
        // 记录结果表最新修改时间，用于 dataStale 判断
        context.setLatestResultUpdateTime(rawResults.stream()
                .map(result -> this.resolveRowTime(result))
                .filter(Objects::nonNull)
                .max(Date::compareTo)
                .orElse(null));
        // 2. 剔除虚拟机台（唯一事实来源）
        List<LhScheduleResult> realResults = LhVirtualMachineUtil.excludeVirtualMachines(
                rawResults, LhScheduleResult::getLhMachineCode);
        if (!rawResults.isEmpty() && realResults.isEmpty()) {
            log.warn("硫化排程校验：排程结果仅含虚拟机台行, 已全部过滤, factoryCode={}, scheduleDate={}",
                    factoryCode, DateUtil.formatDate(scheduleDate));
        }
        // 3. 跨批次去重（同一机台同一物理班只保留最新批次行）
        context.setResults(this.dedupCrossBatch(realResults));

        // 4. 台账数据
        context.setMouldInfoList(this.loadMouldInfoList(factoryCode));
        context.setMouldShellInfoList(this.loadMouldShellInfoList(factoryCode));
        context.setCapsuleChuckList(this.loadCapsuleChuckList(factoryCode));
        Set<String> materialCodes = context.getResults().stream()
                .map(LhScheduleResult::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
        context.setMaterialInfoList(this.loadMaterialInfoList(factoryCode, materialCodes));
        context.setSkuMouldRelList(this.loadSkuMouldRelList(factoryCode, materialCodes));
        context.setCxEndingShiftList(this.loadCxEndingShiftList(factoryCode, scheduleDate));
        return context;
    }

    /**
     * 加载模具台账。
     *
     * @param factoryCode 工厂编号
     * @return 模具台账
     */
    private List<MdmModelInfo> loadMouldInfoList(String factoryCode) {
        return mdmModelInfoMapper.selectList(new LambdaQueryWrapper<MdmModelInfo>()
                .eq(MdmModelInfo::getFactoryCode, factoryCode)
                .eq(MdmModelInfo::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
    }

    /**
     * 加载模壳台账。
     *
     * @param factoryCode 工厂编号
     * @return 模壳台账
     */
    private List<MdmMouldShellInfo> loadMouldShellInfoList(String factoryCode) {
        return mdmMouldShellInfoMapper.selectList(new LambdaQueryWrapper<MdmMouldShellInfo>()
                .eq(MdmMouldShellInfo::getFactoryCode, factoryCode)
                .eq(MdmMouldShellInfo::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
    }

    /**
     * 加载胶囊卡盘台账。
     *
     * @param factoryCode 工厂编号
     * @return 卡盘台账
     */
    private List<MdmCapsuleChuck> loadCapsuleChuckList(String factoryCode) {
        return mdmCapsuleChuckMapper.selectList(new LambdaQueryWrapper<MdmCapsuleChuck>()
                .eq(MdmCapsuleChuck::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
    }

    /**
     * 加载物料主数据（仅结果集涉及物料）。
     *
     * @param factoryCode 工厂编号
     * @param materialCodes 物料编码集合
     * @return 物料主数据
     */
    private List<MdmMaterialInfo> loadMaterialInfoList(String factoryCode, Set<String> materialCodes) {
        if (materialCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return mdmMaterialInfoMapper.selectList(new LambdaQueryWrapper<MdmMaterialInfo>()
                .eq(MdmMaterialInfo::getFactoryCode, factoryCode)
                .in(MdmMaterialInfo::getMaterialCode, materialCodes)
                .eq(MdmMaterialInfo::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
    }

    /**
     * 加载 SKU 模具关系（仅结果集涉及物料）。
     *
     * @param factoryCode 工厂编号
     * @param materialCodes 物料编码集合
     * @return SKU 模具关系
     */
    private List<MdmSkuMouldRel> loadSkuMouldRelList(String factoryCode, Set<String> materialCodes) {
        if (materialCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return mdmSkuMouldRelMapper.selectList(new LambdaQueryWrapper<MdmSkuMouldRel>()
                .eq(MdmSkuMouldRel::getFactoryCode, factoryCode)
                .in(MdmSkuMouldRel::getMaterialCode, materialCodes)
                .eq(MdmSkuMouldRel::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
    }

    /**
     * 加载成型收尾记录（按业务日前后窗口查询，避免精确日期过滤漏数据）。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日
     * @return 成型收尾记录
     */
    private List<CxEmbryoEndingShift> loadCxEndingShiftList(String factoryCode, Date scheduleDate) {
        Date windowStart = DateUtil.offsetDay(DateUtil.beginOfDay(scheduleDate), -CX_ENDING_WINDOW_BEFORE_DAYS);
        Date windowEnd = DateUtil.offsetDay(DateUtil.beginOfDay(scheduleDate), CX_ENDING_WINDOW_AFTER_DAYS);
        return cxEmbryoEndingShiftMapper.selectList(new LambdaQueryWrapper<CxEmbryoEndingShift>()
                .eq(CxEmbryoEndingShift::getFactoryCode, factoryCode)
                .between(CxEmbryoEndingShift::getShiftDate, windowStart, windowEnd)
                .eq(CxEmbryoEndingShift::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
    }

    /**
     * 跨批次去重：同一「机台 + 班次 + 物理班开始时刻」只保留最新批次的行。
     *
     * <p>两遍算法：第一遍按唯一键记录最新行时间；第二遍只要行在任一键上是最新行即保留，
     * 无唯一键的行（开始时刻缺失）一律保留，避免误删。</p>
     *
     * @param results 排程结果
     * @return 去重后的结果
     */
    private List<LhScheduleResult> dedupCrossBatch(List<LhScheduleResult> results) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }
        Map<LhScheduleResult, List<String>> keysByRow = new IdentityHashMap<>();
        Map<String, Date> latestTimeByKey = new HashMap<>(results.size() * 4);
        for (LhScheduleResult result : results) {
            List<String> rowKeys = this.buildRowKeys(result);
            keysByRow.put(result, rowKeys);
            Date rowTime = this.resolveRowTime(result);
            if (rowTime == null) {
                continue;
            }
            for (String rowKey : rowKeys) {
                Date current = latestTimeByKey.get(rowKey);
                if (current == null || rowTime.after(current)) {
                    latestTimeByKey.put(rowKey, rowTime);
                }
            }
        }
        List<LhScheduleResult> dedupedResults = new ArrayList<>(results.size());
        for (LhScheduleResult result : results) {
            List<String> rowKeys = keysByRow.get(result);
            if (rowKeys.isEmpty()) {
                dedupedResults.add(result);
                continue;
            }
            Date rowTime = this.resolveRowTime(result);
            boolean latest = rowKeys.stream().anyMatch(rowKey -> {
                Date latestTime = latestTimeByKey.get(rowKey);
                return latestTime != null && latestTime.equals(rowTime);
            });
            if (latest) {
                dedupedResults.add(result);
            }
        }
        return dedupedResults;
    }

    /**
     * 构建行的跨批次唯一键（机台 + 班次 + 物理班开始时刻）。
     *
     * @param result 排程结果行
     * @return 唯一键列表（开始时刻缺失的班次不产生键）
     */
    private List<String> buildRowKeys(LhScheduleResult result) {
        List<String> rowKeys = new ArrayList<>(LhScheduleChecker.SHIFT_SLOT_COUNT);
        String machineCode = StringUtils.trimToEmpty(result.getLhMachineCode());
        for (int shiftIndex = 1; shiftIndex <= LhScheduleChecker.SHIFT_SLOT_COUNT; shiftIndex++) {
            Date startTime = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
            if (startTime == null) {
                continue;
            }
            rowKeys.add(machineCode + ROW_KEY_SEPARATOR + shiftIndex
                    + ROW_KEY_SEPARATOR + startTime.getTime());
        }
        return rowKeys;
    }

    /**
     * 取行的修改时间（updateTime 与 createTime 的较大值）。
     *
     * @param result 排程结果行
     * @return 修改时间；两者皆空返回 null
     */
    private Date resolveRowTime(LhScheduleResult result) {
        Date createTime = result.getCreateTime();
        Date updateTime = result.getUpdateTime();
        if (createTime == null && updateTime == null) {
            return null;
        }
        if (createTime == null) {
            return updateTime;
        }
        if (updateTime == null) {
            return createTime;
        }
        return updateTime.after(createTime) ? updateTime : createTime;
    }

    /**
     * 取触发批次号（结果集首个非空批次号，仅作追溯）。
     *
     * @param results 排程结果
     * @return 批次号，可能为空
     */
    private String resolveBatchNo(List<LhScheduleResult> results) {
        if (results == null) {
            return null;
        }
        return results.stream()
                .map(LhScheduleResult::getBatchNo)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    /**
     * 查询指定工厂业务日的有效校验记录。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日
     * @return 校验记录（按校验时间升序）
     */
    private List<LhScheduleCheckResult> listRecords(String factoryCode, Date scheduleDate) {
        return this.list(new LambdaQueryWrapper<LhScheduleCheckResult>()
                .eq(LhScheduleCheckResult::getFactoryCode, factoryCode)
                .eq(LhScheduleCheckResult::getScheduleDate, scheduleDate)
                .eq(LhScheduleCheckResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .orderByAsc(LhScheduleCheckResult::getCheckTime)
                .orderByAsc(LhScheduleCheckResult::getId));
    }

    /**
     * 判断校验数据是否已过期：校验之后结果表被修改过。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日
     * @param checkTime 最近校验时间
     * @return true-已过期
     */
    private boolean resolveDataStale(String factoryCode, Date scheduleDate, Date checkTime) {
        if (checkTime == null) {
            return false;
        }
        Long changedCount = lhScheduleResultMapper.selectCount(new LambdaQueryWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getFactoryCode, factoryCode)
                .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .and(wrapper -> wrapper
                        .gt(LhScheduleResult::getUpdateTime, checkTime)
                        .or()
                        .gt(LhScheduleResult::getCreateTime, checkTime)));
        return changedCount != null && changedCount > 0;
    }

    /**
     * 取当前登录用户名（无登录上下文时回退 system）。
     *
     * @return 用户名
     */
    private String resolveCurrentUserName() {
        try {
            return StringUtils.defaultIfBlank(SecurityUtils.getUsername(), SYSTEM_USER);
        } catch (Exception e) {
            return SYSTEM_USER;
        }
    }
}
