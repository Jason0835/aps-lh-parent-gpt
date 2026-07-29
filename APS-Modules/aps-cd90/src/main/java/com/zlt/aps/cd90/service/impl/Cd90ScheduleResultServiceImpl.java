package com.zlt.aps.cd90.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleRollingAdjustLog;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.api.domain.vo.Cd90ChangeQtyRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90RollingCheckRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90TransferMachineRequest;
import com.zlt.aps.cd90.component.Cd90ScheduleResultExportAssembler;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskType;
import com.zlt.aps.cd90.engine.model.Cd90BatchDataCheckResult;
import com.zlt.aps.cd90.engine.model.Cd90InsertCarryoverImpact;
import com.zlt.aps.cd90.engine.model.Cd90InsertRollingOutput;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleBatchDataValidator;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleLockService;
import com.zlt.aps.cd90.engine.service.Cd90InsertRollingService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleShiftMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineConstructionMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineCxScheduleMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleRollingAdjustLogMapper;
import com.zlt.aps.cd90.mapper.Cd90UnscheduleResultMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.mapper.Cd90StockMapper;
import com.zlt.aps.cd90.model.Cd90ScheduleOverwriteDecision;
import com.zlt.aps.cd90.service.Cd90AutoScheduleAsyncExecutor;
import com.zlt.aps.cd90.service.Cd90InsertOrderAsyncExecutor;
import com.zlt.aps.cd90.service.Cd90ScheduleOverwriteValidator;
import com.zlt.aps.cd90.service.Cd90TimedRollingCheckService;
import com.zlt.aps.cd90.service.ICd90ScheduleResultService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.common.utils.PubUtil;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(rollbackFor = Exception.class)
/**
 * 直裁排程结果服务实现。
 * 负责自动排程、插单、转机台、调量、定时滚动校验等业务入口编排，
 * 并对接排程任务、滚动重排引擎、批次级数据校验和任务状态查询能力。
 */
public class Cd90ScheduleResultServiceImpl extends AbstractDocService<Cd90ScheduleResult> implements ICd90ScheduleResultService {

    /** 删除完成量校验覆盖数据库保留的 CLASS1 至 CLASS8。 */
    private static final int CLASS_COUNT = 8;

    /**
     * 服务内部依赖的 Mapper、校验器、异步执行器和引擎入口。
     * 分别负责排程结果读写、任务编排、批次校验、滚动预演与施工信息补充查询。
     */
    @Resource
    private Cd90ScheduleResultMapper cd90ScheduleResultMapper;
    @Resource
    private Cd90ScheduleOverwriteValidator overwriteValidator;
    @Resource
    private Cd90ScheduleTaskService taskService;
    @Resource
    private Cd90AutoScheduleAsyncExecutor asyncExecutor;
    @Resource
    private Cd90AutoScheduleBatchDataValidator batchDataValidator;
    @Resource
    private Cd90AutoScheduleShiftMapper shiftMapper;
    @Resource
    private Cd90InsertOrderAsyncExecutor insertOrderAsyncExecutor;
    @Resource
    private Cd90ScheduleRollingAdjustLogMapper rollingAdjustLogMapper;
    @Resource
    private Cd90UnscheduleResultMapper unscheduleResultMapper;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private Cd90TimedRollingCheckService timedRollingCheckService;
    @Resource
    private Cd90EngineConstructionMapper constructionMapper;
    @Resource
    private Cd90InsertRollingService insertRollingService;
    @Resource
    private Cd90AutoScheduleLockService lockService;
    @Resource
    private Cd90StockMapper cd90StockMapper;
    @Resource
    private Cd90EngineCxScheduleMapper cxScheduleMapper;
    @Resource
    private Cd90ScheduleResultExportAssembler exportAssembler;

    /**
     * 删除直裁排程结果，并在同一事务内压缩 CLASS1 后续生产顺位。
     * 删除过程不调用滚动重排，其他班次顺位保持不变。
     *
     * @param ids 待删除排程结果主键
     * @return 删除结果
     */
    @Override
    public AjaxResult removeScheduleResults(List<Long> ids) {
        // 过滤空主键并去重，避免重复 ID 影响删除数量校验。
        List<Long> deleteIds = ids == null ? Collections.emptyList() : ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (deleteIds.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.message.parameter.error"));
        }
        // 首次查询用于确认记录存在，并提取需要加锁的工厂和排程日期范围。
        List<Cd90ScheduleResult> selected = this.selectDeleteResults(deleteIds);
        if (selected.size() != deleteIds.size()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.message.parameter.error"));
        }
        // 多日期批量删除按固定顺序获取锁，避免并发请求交叉等待。
        List<Cd90ScheduleResult> scopeSamples = new ArrayList<>(selected.stream()
                .collect(Collectors.toMap(this::scheduleScopeKey,
                        result -> result, (first, second) -> first,
                        LinkedHashMap::new)).values());
        scopeSamples.sort(Comparator
                .comparing(Cd90ScheduleResult::getFactoryCode,
                        Comparator.nullsFirst(String::compareTo))
                .thenComparing(Cd90ScheduleResult::getScheduleDate,
                        Comparator.nullsFirst(Date::compareTo)));

        // 与自动排程共用“工厂 + 排程日期”锁，防止删除和排程写入并发执行。
        List<RLock> acquiredLocks = new ArrayList<>();
        boolean releaseAfterTransaction = false;
        try {
            for (Cd90ScheduleResult scope : scopeSamples) {
                if (this.isBlank(scope.getFactoryCode())
                        || scope.getScheduleDate() == null) {
                    return AjaxResult.error(I18nUtil.getMessage(
                            "ui.message.parameter.error"));
                }
                RLock lock = lockService.getLock(scope.getFactoryCode(),
                        this.toLocalDate(scope.getScheduleDate()));
                if (!lock.tryLock()) {
                    return AjaxResult.error(I18nUtil.getMessage(
                            "ui.cd90.schedule.taskActive"));
                }
                acquiredLocks.add(lock);
            }
            // 锁需覆盖事务提交或回滚，避免数据库尚未提交时其他排程任务进入。
            releaseAfterTransaction = this.releaseLocksAfterTransaction(
                    acquiredLocks);

            // 加锁后重新查询，防止等待锁期间记录状态被其他事务修改。
            selected = this.selectDeleteResults(deleteIds);
            if (selected.size() != deleteIds.size()) {
                return AjaxResult.error(I18nUtil.getMessage(
                        "ui.message.parameter.error"));
            }
            // 锁内复核待执行或执行中的排程任务，避免删除正在被任务使用的数据。
            for (Cd90ScheduleResult scope : scopeSamples) {
                if (taskService.findActive(scope.getFactoryCode(),
                        scope.getScheduleDate()) != null) {
                    return AjaxResult.error(I18nUtil.getMessage(
                            "ui.cd90.schedule.taskActive"));
                }
            }
            // 已发布成功或任一班次已有完成量的记录均不允许删除。
            AjaxResult validation = this.validateDeleteResults(selected);
            if (validation != null) {
                return validation;
            }
            // 主表使用框架逻辑删除；数量不一致时抛错并回滚整个事务。
            int deletedCount = this.removeByIds(deleteIds);
            if (deletedCount != deleteIds.size()) {
                throw new IllegalStateException(I18nUtil.getMessage(
                        "ui.message.operation.failed"));
            }
            // 删除成功后只压缩同机台 CLASS1 后续生产顺位，不触发滚动重排。
            this.compactClass1ProduceOrders(selected);
            return AjaxResult.success(I18nUtil.getMessage(
                    "ui.message.operation.success"));
        } finally {
            if (!releaseAfterTransaction) {
                // 事务回调尚未注册时，由当前线程兜底释放已获得的锁。
                this.unlockDeleteLocks(acquiredLocks);
            }
        }
    }

    /** 查询待删除且尚未逻辑删除的排程结果。 */
    private List<Cd90ScheduleResult> selectDeleteResults(List<Long> ids) {
        return cd90ScheduleResultMapper.selectList(
                new LambdaQueryWrapper<Cd90ScheduleResult>()
                        .in(Cd90ScheduleResult::getId, ids));
    }

    /** 校验已发布成功和已有完成量两项删除限制。 */
    private AjaxResult validateDeleteResults(List<Cd90ScheduleResult> selected) {
        boolean published = selected.stream().anyMatch(result ->
                result.getPublishSuccessCount() != null
                        && result.getPublishSuccessCount() > 0);
        if (published) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.data.column.cd90ScheduleResult.hasPublishedCanNotDelete"));
        }
        if (selected.stream().anyMatch(this::hasFinishQuantity)) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd90.scheduleResult.finishQtyCannotDelete"));
        }
        return null;
    }

    /** CLASS1 至 CLASS8 任一班次有正完成量时禁止删除。 */
    private boolean hasFinishQuantity(Cd90ScheduleResult result) {
        return IntStream.rangeClosed(1, CLASS_COUNT)
                .mapToObj(classIndex -> result.getFieldValueByFieldName(
                        String.format("class%dFinishQty", classIndex)))
                .filter(Objects::nonNull)
                .map(Number.class::cast)
                .anyMatch(finishQuantity -> finishQuantity.doubleValue() > 0D);
    }

    /** 删除后仅压缩 CLASS1 后续生产顺位，其他班次不调整。 */
    private void compactClass1ProduceOrders(
            List<Cd90ScheduleResult> deletedResults) {
        Map<String, List<Cd90ScheduleResult>> deletedByScope = deletedResults
                .stream()
                .filter(result -> result.getClass1ProduceOrder() != null
                        && result.getClass1ProduceOrder() > 0)
                .collect(Collectors.groupingBy(this::class1OrderScopeKey,
                        LinkedHashMap::new, Collectors.toList()));
        deletedByScope.values().forEach(this::compactClass1ScopeOrders);
    }

    /** 压缩单个工厂、日期、机台范围内被删除顺位之后的 CLASS1 顺位。 */
    private void compactClass1ScopeOrders(
            List<Cd90ScheduleResult> deletedScopeResults) {
        Cd90ScheduleResult sample = deletedScopeResults.get(0);
        LambdaQueryWrapper<Cd90ScheduleResult> queryWrapper =
                new LambdaQueryWrapper<Cd90ScheduleResult>()
                        .eq(Cd90ScheduleResult::getFactoryCode,
                                sample.getFactoryCode())
                        .eq(Cd90ScheduleResult::getScheduleDate,
                                sample.getScheduleDate())
                        .isNotNull(Cd90ScheduleResult::getClass1ProduceOrder)
                        .gt(Cd90ScheduleResult::getClass1ProduceOrder, 0)
                        .orderByAsc(Cd90ScheduleResult::getClass1ProduceOrder)
                        .orderByAsc(Cd90ScheduleResult::getId);
        if (sample.getMachineCode() == null) {
            queryWrapper.isNull(Cd90ScheduleResult::getMachineCode);
        } else {
            queryWrapper.eq(Cd90ScheduleResult::getMachineCode,
                    sample.getMachineCode());
        }
        List<Cd90ScheduleResult> remaining = cd90ScheduleResultMapper.selectList(
                queryWrapper);
        // 同一顺位仍有其他记录时保留该顺位，只移除已完全空缺的顺位值。
        Set<Integer> removedOrders = deletedScopeResults.stream()
                .map(Cd90ScheduleResult::getClass1ProduceOrder)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> remainingOrders = remaining.stream()
                .map(Cd90ScheduleResult::getClass1ProduceOrder)
                .collect(Collectors.toSet());
        removedOrders.removeAll(remainingOrders);
        if (removedOrders.isEmpty()) {
            return;
        }
        remaining.forEach(result -> {
            Integer currentOrder = result.getClass1ProduceOrder();
            long removedBefore = removedOrders.stream()
                    .filter(removedOrder -> removedOrder < currentOrder)
                    .count();
            if (removedBefore <= 0) {
                return;
            }
            int targetOrder = currentOrder - (int) removedBefore;
            cd90ScheduleResultMapper.update(null,
                    new LambdaUpdateWrapper<Cd90ScheduleResult>()
                            .set(Cd90ScheduleResult::getClass1ProduceOrder,
                                    targetOrder)
                            .eq(Cd90ScheduleResult::getId,
                                    result.getId()));
        });
    }

    /** 在当前事务结束后释放删除持有的排程锁。 */
    private boolean releaseLocksAfterTransaction(List<RLock> acquiredLocks) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        List<RLock> locksToRelease = new ArrayList<>(acquiredLocks);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        Cd90ScheduleResultServiceImpl.this.unlockDeleteLocks(
                                locksToRelease);
                    }
                });
        return true;
    }

    /** 按获取逆序释放删除排程锁。 */
    private void unlockDeleteLocks(List<RLock> acquiredLocks) {
        for (int index = acquiredLocks.size() - 1; index >= 0; index--) {
            RLock lock = acquiredLocks.get(index);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 构造工厂和排程日期维度的锁排序键。 */
    private String scheduleScopeKey(Cd90ScheduleResult result) {
        return String.valueOf(result.getFactoryCode()) + "|"
                + String.valueOf(result.getScheduleDate());
    }

    /** 构造 CLASS1 顺位压缩范围键。 */
    private String class1OrderScopeKey(Cd90ScheduleResult result) {
        return this.scheduleScopeKey(result) + "|"
                + String.valueOf(result.getMachineCode());
    }

    /** 将数据库排程日期转换为分布式锁使用的本地日期。 */
    private LocalDate toLocalDate(Date scheduleDate) {
        return scheduleDate.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * 使用固定模板导出直裁四班排程结果。
     * <p>
     * 流程：校验导出条件 → 加载前一日排程结果 → 解析早班编码 → 加载前一日库存
     * → 加载当日成型排程 → 加载施工BOM → 组装数据行 → 读取模板 → 写入Excel。
     *
     * @param currentResults 已按现有导出条件查询的本批排程结果
     * @param queryVO 导出条件（含工厂、排程日期等）
     * @return Excel文件字节
     * @throws ServiceException 模板文件不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportData(List<Cd90ScheduleResult> currentResults, Cd90ScheduleResult queryVO) {
        // 校验导出条件
        if (queryVO == null || !PubUtil.isNotEmpty(queryVO.getFactoryCode())
                || queryVO.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.column.cd90ScheduleResult.exportRequired"));
        }
        // 排程日期的前一天，用于加载前一日排程结果和库存
        Date previousDate = DateUtil.offsetDay(queryVO.getScheduleDate(), -1);
        // 加载前一日排程结果（用于计算前一日各班实际剩余量）
        List<Cd90ScheduleResult> previousResults = this.loadPreviousResults(queryVO, previousDate);
        // 解析工厂早班班次编码
        String earlyShiftCode = this.resolveEarlyShiftCode(queryVO.getFactoryCode());
        // 加载前一日早班起至当日早班前的库存
        List<Cd90Stock> stocks = this.loadStocks(
                queryVO.getFactoryCode(), previousDate, earlyShiftCode);
        // 加载当日成型排程结果（用于计算施工BOM需求）
        List<CxScheduleResult> formingResults = this.loadFormingResults(
                queryVO.getFactoryCode(), queryVO.getScheduleDate());
        // 加载施工BOM，获取直裁宽度、大卷幅宽等参数
        List<MdmConstructionInfo> constructions = this.loadConstructions(
                queryVO.getFactoryCode(), formingResults);
        // 组装导出数据行：前日排程 + 当日排程 + 库存 + 成型需求 + 施工BOM
        List<Map<String, Object>> rows = this.exportAssembler.assembleRows(
                previousResults, currentResults, stocks, formingResults, constructions);

        // 读取Excel模板
        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cd90ScheduleResult.xlsx");
        if (inputStream == null) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.column.cd90ScheduleResult.exportTemplateNotFound"));
        }
        // 将数据写入模板并返回字节流
        Map<String, Object> tableMap = new HashMap<>(this.exportAssembler.buildTableMap(queryVO.getScheduleDate()));
        List<List<Map<String, Object>>> excelDataList = Collections.singletonList(rows);
        byte[] bytes = ExcelUtils.writeMultiList(
                inputStream,
                0,
                tableMap,
                excelDataList);
        return bytes;
    }

    /**
     * 加载前一日排程结果。
     * 按工厂编码、前一日日期查询排程记录，可选按帘布代号、机台编码、发布状态过滤。
     * 结果按机台编码、大卷编码、各班生产顺序升序排列，用于导出时计算前日剩余量。
     *
     * @param queryVO     导出条件查询对象
     * @param previousDate 前一日日期
     * @return 前一日排程结果列表
     */
    private List<Cd90ScheduleResult> loadPreviousResults(Cd90ScheduleResult queryVO,
                                                         Date previousDate) {
        LambdaQueryWrapper<Cd90ScheduleResult> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Cd90ScheduleResult::getFactoryCode, queryVO.getFactoryCode());
        wrapper.eq(Cd90ScheduleResult::getScheduleDate, previousDate);
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getClothCode()),
                Cd90ScheduleResult::getClothCode, queryVO.getClothCode());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()),
                Cd90ScheduleResult::getMachineCode, queryVO.getMachineCode());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getIsRelease()),
                Cd90ScheduleResult::getIsRelease, queryVO.getIsRelease());
        wrapper.orderByAsc(Cd90ScheduleResult::getMachineCode);
        wrapper.orderByAsc(Cd90ScheduleResult::getBigRollCode);
        wrapper.orderByAsc(Cd90ScheduleResult::getClass3ProduceOrder);
        return this.cd90ScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 解析工厂早班（CLASS3）的班次编码。
     * 按班次排序取第一个有效的班次编码，用于加载前一日早班库存数据。
     *
     * @param factoryCode 工厂编码
     * @return 早班班次编码，无配置时返回 null
     */
    private String resolveEarlyShiftCode(String factoryCode) {
        return this.shiftMapper.selectList(Wrappers.<Cd90ShiftConfig>lambdaQuery()
                        .eq(Cd90ShiftConfig::getFactoryCode, factoryCode)
                        .eq(Cd90ShiftConfig::getIsActive, 1)
                        .eq(Cd90ShiftConfig::getClassField, "CLASS3")
                        .orderByAsc(Cd90ShiftConfig::getScheduleDay)
                        .orderByAsc(Cd90ShiftConfig::getDayShiftOrder)
                        .orderByAsc(Cd90ShiftConfig::getShiftOrder))
                .stream()
                .map(Cd90ShiftConfig::getShiftCode)
                .filter(PubUtil::isNotEmpty)
                .findFirst()
                .orElse(null);
    }

    /**
     * 加载指定日期和班次的库存数据。
     * 按工厂编码、库存日期、班次编码查询，结果按物料编码升序排列。
     *
     * @param factoryCode 工厂编码
     * @param stockDate   库存日期
     * @param shiftCode   班次编码
     * @return 库存列表，班次编码为空时返回空列表
     */
    private List<Cd90Stock> loadStocks(String factoryCode, Date stockDate, String shiftCode) {
        if (!PubUtil.isNotEmpty(shiftCode)) {
            return Collections.emptyList();
        }
        return this.cd90StockMapper.selectList(Wrappers.<Cd90Stock>lambdaQuery()
                .eq(Cd90Stock::getFactoryCode, factoryCode)
                .eq(Cd90Stock::getStockDate, stockDate)
                .eq(Cd90Stock::getShiftCode, shiftCode)
                .orderByAsc(Cd90Stock::getMaterialCode));
    }

    /**
     * 加载指定日期的成型排程结果。
     * 用于导出时关联成型计划的胎胚编号、配方版本等信息。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 成型排程结果列表，按胎胚编号和ID升序排列
     */
    private List<CxScheduleResult> loadFormingResults(String factoryCode, Date scheduleDate) {
        return this.cxScheduleMapper.selectList(Wrappers.<CxScheduleResult>lambdaQuery()
                .eq(CxScheduleResult::getFactoryCode, factoryCode)
                .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                .orderByAsc(CxScheduleResult::getEmbryoCode)
                .orderByAsc(CxScheduleResult::getId));
    }

    /**
     * 加载施工BOM信息。
     * 从成型排程结果中提取施工编号和施工版本，批量查询施工信息主数据，
     * 用于获取直裁宽度、大卷幅宽等导出所需参数。
     *
     * @param factoryCode    工厂编码
     * @param formingResults 成型排程结果列表
     * @return 施工信息列表，按施工编号和版本升序排列
     */
    private List<MdmConstructionInfo> loadConstructions(String factoryCode,
                                                         List<CxScheduleResult> formingResults) {
        Set<String> constructionCodes = formingResults.stream()
                .map(CxScheduleResult::getEmbryoCode)
                .filter(PubUtil::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> constructionVersions = formingResults.stream()
                .flatMap(result -> Arrays.asList(
                        result.getClass1RecipeNo(),
                        result.getClass2RecipeNo(),
                        result.getClass3RecipeNo(),
                        result.getClass4RecipeNo()).stream())
                .filter(PubUtil::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (constructionCodes.isEmpty() || constructionVersions.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<MdmConstructionInfo> constructionWrapper = new LambdaQueryWrapper<>();
        constructionWrapper.eq(MdmConstructionInfo::getFactoryCode, factoryCode);
        constructionWrapper.in(MdmConstructionInfo::getConstructionCode, constructionCodes);
        constructionWrapper.in(MdmConstructionInfo::getConstructionVersion, constructionVersions);
        constructionWrapper.orderByAsc(MdmConstructionInfo::getConstructionCode);
        constructionWrapper.orderByAsc(MdmConstructionInfo::getConstructionVersion);
        return this.constructionMapper.selectList(constructionWrapper);
    }

    /**
     * 接收自动排程请求。
     * 排程算法统一由Aps-Engine中的直裁引擎实现，本服务只负责业务接口转发。
     *
     * @param scheduleResult 自动排程条件，当前使用工厂编码和排程日期
     * @return 接口调用成功
     */
    @Override
    public AjaxResult autoSchedule(Cd90ScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            // 信息：自动排程请求不能为空
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.autoSchedule.planRequestEmpty"));
        }
        if (scheduleResult.getFactoryCode() == null || scheduleResult.getFactoryCode().trim().isEmpty()
                || scheduleResult.getScheduleDate() == null) {
            // 信息：自动排程工厂编码和排程日期不能为空
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.autoSchedule.factoryAndDateEmpty"));
        }
        // 正式进入自动排程前，同步做1.2节批次级数据先行检查；
        // 失败时不创建PENDING任务、不占用执行锁、不进入异步执行器，直接返回结构化错误。
        LocalDate localScheduleDate = scheduleResult.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Cd90BatchDataCheckResult batchCheck = batchDataValidator.check(
                scheduleResult.getFactoryCode(), localScheduleDate);
        if (batchCheck.isFailed()) {
            // 走success+batchCheckFailed标记，避免HTTP 500被前端拦截器拦截且丢失data；
            // 与needConfirm模式一致，由前端按data.batchCheckFailed分流渲染结构化错误。
            Map<String, Object> data = new HashMap<>();
            data.put("needConfirm", false);
            data.put("batchCheckFailed", true);
            data.put("errors", toErrorList(batchCheck.getErrors()));
            data.put("warnings", toErrorList(batchCheck.getWarnings()));
            return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
        }
        List<Cd90ScheduleResult> existing = cd90ScheduleResultMapper.selectList(
                new LambdaQueryWrapper<Cd90ScheduleResult>()
                        .eq(Cd90ScheduleResult::getFactoryCode, scheduleResult.getFactoryCode())
                        .eq(Cd90ScheduleResult::getScheduleDate, scheduleResult.getScheduleDate()));
        Cd90ScheduleOverwriteDecision decision = overwriteValidator.validate(existing,
                Boolean.TRUE.equals(scheduleResult.getForceRegenerate()));
        if (decision.isRejected()) {
            return AjaxResult.error(decision.getMessage());
        }
        Map<String, Object> data = new HashMap<>();
        if (decision.isNeedConfirm()) {
            data.put("needConfirm", true);
            return AjaxResult.success(decision.getMessage(), data);
        }
        // 先查询当前工厂、当前排程日期是否已有进行中的自动排程任务，
        // 如果存在则直接返回已有任务ID，避免重复提交相同日期的异步任务。
        Cd90ScheduleTask activeTask = taskService.findActive(
                scheduleResult.getFactoryCode(), scheduleResult.getScheduleDate());
        if (activeTask != null) {
            data.put("needConfirm", false);
            data.put("taskId", activeTask.getTaskId());
            // 信息：当前日期已有自动排程任务正在执行
            return AjaxResult.success(I18nUtil.getMessage("ui.cd90.autoSchedule.activeTask"), data);
        }

        // 组装本次触发请求的快照信息，并创建一条 PENDING 状态的自动排程任务记录，
        // 便于异步执行链路进行状态跟踪、异常回溯和任务审计。
        String snapshot = "factoryCode=" + scheduleResult.getFactoryCode()
                + ",scheduleDate=" + scheduleResult.getScheduleDate()
                + ",forceRegenerate=" + Boolean.TRUE.equals(scheduleResult.getForceRegenerate());
        Cd90ScheduleTask task = taskService.createPending(scheduleResult.getFactoryCode(),
                scheduleResult.getScheduleDate(), Cd90ScheduleTaskType.AUTO_SCHEDULE,
                "MANUAL", snapshot, null);

        // 将任务投递到异步执行器中实际启动排程计算，并把新任务ID返回给前端，
        // 前端后续可通过 taskId 轮询任务状态和排程结果。
        asyncExecutor.execute(task.getTaskId(), task.getFactoryCode(), task.getScheduleDate());
        data.put("needConfirm", false);
        data.put("taskId", task.getTaskId());
        // 信息：自动排程任务已提交
        return AjaxResult.success(I18nUtil.getMessage("ui.cd90.autoSchedule.taskSubmitted"), data);
    }
    /**
     * 计算指定排程日对应的各班次时间信息。
     *
     * @param request 插单请求，至少包含工厂和排程日期
     * @return 班次日期、起止时间、是否当前班次以及是否允许调量的信息
    */
    @Override
    public AjaxResult shiftDates(Cd90InsertOrderRequest request) {
        if (request == null || request.getScheduleDate() == null
                || request.getFactoryCode() == null || request.getFactoryCode().trim().isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.required"));
        }
        LocalDate scheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> values = this.activeShiftConfigs(request.getFactoryCode())
                .stream()
                .map(config -> {
                    LocalDate shiftDate = scheduleDate.plusDays(this.scheduleDay(config) - 2L);
                    LocalDateTime startTime = LocalDateTime.of(shiftDate, LocalTime.parse(config.getStartTime()));
                    LocalDateTime endTime = this.resolveShiftEnd(shiftDate, config);
                    Map<String, Object> item = new HashMap<>();
                    item.put("classField", config.getClassField());
                    item.put("shiftCode", config.getShiftCode());
                    item.put("shiftName", config.getShiftName());
                    item.put("shiftDate", shiftDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    item.put("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    item.put("endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    item.put("currentShift", !now.isBefore(startTime) && now.isBefore(endTime));
                    item.put("changeQtyEditable", now.isBefore(endTime));
                    return item;
                }).collect(Collectors.toList());
        return AjaxResult.success(values);
    }

    /**
     * 查询工厂所有启用的班次配置。
     * 按 scheduleDay、dayShiftOrder、shiftOrder 升序排列，用于计算班次时间和可编辑窗口。
     *
     * @param factoryCode 工厂编码
     * @return 启用状态的班次配置列表
     */
    private List<Cd90ShiftConfig> activeShiftConfigs(String factoryCode) {
        return shiftMapper.selectList(
                        new LambdaQueryWrapper<Cd90ShiftConfig>()
                                .eq(Cd90ShiftConfig::getFactoryCode, factoryCode)
                                .eq(Cd90ShiftConfig::getIsActive, 1))
                .stream()
                .sorted(Comparator.comparing(Cd90ShiftConfig::getScheduleDay)
                        .thenComparing(Cd90ShiftConfig::getDayShiftOrder)
                        .thenComparing(Cd90ShiftConfig::getShiftOrder))
                .collect(Collectors.toList());
    }

    /**
     * 获取班次配置的排程日偏移值。
     * 若配置中 scheduleDay 为空则默认返回2，表示该班次归属排程日的第二天。
     *
     * @param config 班次配置
     * @return 排程日偏移值
     */
    private int scheduleDay(Cd90ShiftConfig config) {
        return config.getScheduleDay() == null ? 2 : config.getScheduleDay();
    }

    /**
     * 计算班次结束时间。
     * 根据 isCrossDay 标志判断是否跨日：跨日时结束日期加1天，否则与开始日期相同。
     *
     * @param shiftDate 班次开始日期
     * @param config    班次配置
     * @return 班次结束日期时间
     */
    private LocalDateTime resolveShiftEnd(LocalDate shiftDate, Cd90ShiftConfig config) {
        LocalDate endDate = Integer.valueOf(1).equals(config.getIsCrossDay())
                ? shiftDate.plusDays(1) : shiftDate;
        return LocalDateTime.of(endDate, LocalTime.parse(config.getEndTime()));
    }

    /**
     * 解析当前可编辑的班次起始索引。
     * 遍历所有启用班次，找到当前时间尚未结束的第一个班次，返回其 CLASS 编号。
     * 所有班次均已结束时返回7（超出1-6范围），表示排程窗口已关闭。
     *
     * @param scheduleDateValue 排程日期
     * @param factoryCode       工厂编码
     * @return 可编辑班次索引（1-6），窗口关闭时返回7
     */
    private int resolveChangeQtyEditableFromClassIndex(Date scheduleDateValue, String factoryCode) {
        LocalDate scheduleDate = scheduleDateValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDateTime now = LocalDateTime.now();
        return this.activeShiftConfigs(factoryCode).stream()
                .filter(config -> now.isBefore(this.resolveShiftEnd(
                        scheduleDate.plusDays(this.scheduleDay(config) - 2L), config)))
                .map(Cd90ShiftConfig::getClassField)
                .map(this::parseClassIndex)
                .findFirst().orElse(7);
    }
    /**
     * 校验插单请求是否合法。
     *
     * @param request 插单请求
     * @return 校验结果
    */
    @Override
    public AjaxResult validateInsert(Cd90InsertOrderRequest request) {
        if (request == null || request.getScheduleDate() == null
                || isBlank(request.getFactoryCode()) || isBlank(request.getMachineCode())
                || isBlank(request.getClothCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.required"));
        }
        boolean hasPlan = false;
        List<Cd90ScheduleResult> existing = this.selectByDateAndFactory(
                request.getScheduleDate(), request.getFactoryCode());
        for (int classIndex = 1; classIndex <= 6; classIndex++) {
            Double planQuantity = (Double) request.getFieldValueByFieldName(
                    String.format("class%dPlanQty", classIndex));
            Integer produceOrder = (Integer) request.getFieldValueByFieldName(
                    String.format("class%dProduceOrder", classIndex));
            boolean positivePlan = planQuantity != null && planQuantity > 0D;
            if (positivePlan != (produceOrder != null && produceOrder > 0)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.pairRequired"));
            }
            if (!positivePlan) {
                continue;
            }
            hasPlan = true;
            int finalClassIndex = classIndex;
            int finalClassIndex1 = classIndex;
            int highestLockedOrder = existing.stream()
                    .filter(item -> request.getMachineCode().equals(item.getMachineCode()))
                    .filter(item -> isLocked(item, finalClassIndex))
                    .map(item -> readProduceOrder(item, finalClassIndex1))
                    .filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
            if (produceOrder <= highestLockedOrder) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.lockedPrefix"));
            }
            int finalClassIndex2 = classIndex;
            boolean duplicateSegment = existing.stream()
                    .filter(item -> request.getMachineCode().equals(item.getMachineCode()))
                    .filter(item -> request.getClothCode().equals(item.getClothCode()))
                    .anyMatch(item -> readPlanQuantity(item, finalClassIndex2) > 0D);
            if (duplicateSegment) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.duplicateSegment"));
            }
        }
        return hasPlan ? AjaxResult.success()
                : AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.planRequired"));
    }
    /**
     * 提交插单滚动重排任务。
     *
     * @param request 插单请求
     * @return 提交结果，必要时返回跨班顺延确认信息
    */
    @Override
    public AjaxResult insertOrder(Cd90InsertOrderRequest request) {
        AjaxResult validation = this.validateInsert(request);
        if (!Integer.valueOf(200).equals(validation.get("code"))) {
            return validation;
        }
        // 创建INSERT_ORDER异步任务前，复用自动排程1.2节批次级数据先行检查；
        // 失败时不创建PENDING任务、不占用执行锁、不进入异步执行器，
        // 与autoSchedule一致返回success+batchCheckFailed结构化错误，由前端渲染。
        LocalDate localScheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Cd90BatchDataCheckResult batchCheck = batchDataValidator.check(
                request.getFactoryCode(), localScheduleDate);
        if (batchCheck.isFailed()) {
            Map<String, Object> data = new HashMap<>();
            data.put("batchCheckFailed", true);
            data.put("errors", toErrorList(batchCheck.getErrors()));
            data.put("warnings", toErrorList(batchCheck.getWarnings()));
            return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
        }
        // 追加针对插窗帘布的 TIRE_FABRIC_LENGTH/TIRE_FABRIC_CRAFT 检查。
        // batchDataValidator.check 基于成型计划胚号+版本维度校验施工，
        // 插单以单独帘布代号指定，需按该帘布代号兜底校验施工层位中的直裁宽度和单耗。
        Map<String, Object> clothCheckResult = checkInsertClothTireFabric(request.getFactoryCode(), request.getClothCode());
        if (clothCheckResult != null) {
            return AjaxResult.success("帘布 " + request.getClothCode() + " 施工数据检查失败", clothCheckResult);
        }
        Cd90ScheduleTask activeTask = taskService.findActive(
                request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
        }
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            AjaxResult previewResult = this.previewInsertOrder(request, localScheduleDate);
            if (previewResult != null) {
                return previewResult;
            }
        }
        Cd90ScheduleTask task = taskService.createPending(request.getFactoryCode(),
                request.getScheduleDate(), Cd90ScheduleTaskType.INSERT_ORDER,
                "MANUAL", request.toString(), null);
        insertOrderAsyncExecutor.execute(task.getTaskId(), request);
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getTaskId());
        return AjaxResult.success(I18nUtil.getMessage("ui.cd90.insert.submitted"), data);
    }

    /**
     * 使用正式滚动内核执行只读预演，跨班顺延时返回确认明细。
     * 获取分布式锁后执行滚动排程，若产生跨班顺延影响则返回 needConfirm 结构，
     * 由前端展示顺延详情让用户确认后正式提交。
     *
     * @param request      插单请求
     * @param scheduleDate 排程日期
     * @return 有跨班顺延时返回确认信息，无顺延时返回 null，获取锁失败时返回错误
     */
    private AjaxResult previewInsertOrder(Cd90InsertOrderRequest request,
                                          LocalDate scheduleDate) {
        RLock lock = lockService.getLock(request.getFactoryCode(), scheduleDate);
        try {
            if (!lock.tryLock()) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
            }
            if (taskService.findActive(request.getFactoryCode(), request.getScheduleDate()) != null) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
            }
            Cd90InsertRollingOutput output = insertRollingService.execute(request);
            List<Cd90InsertCarryoverImpact> impacts = output.getCarryoverImpacts() == null
                    ? Collections.emptyList() : output.getCarryoverImpacts();
            if (impacts.isEmpty()) {
                return null;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("needConfirm", true);
            data.put("carryoverDetails", impacts.stream()
                    .map(this::toCarryoverDetail)
                    .collect(Collectors.toList()));
            return AjaxResult.success(
                    I18nUtil.getMessage("ui.cd90.insert.carryoverConfirm"), data);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 将引擎影响模型转换为前端确认结构。 */
    private Map<String, Object> toCarryoverDetail(Cd90InsertCarryoverImpact impact) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("clothCode", impact.getClothCode());
        detail.put("affectedType", impact.getAffectedType());
        detail.put("sourceClassField", impact.getSourceClassField());
        detail.put("targetClassField", impact.getTargetClassField());
        detail.put("carryoverQty", impact.getCarryoverQty());
        detail.put("reasonCode", impact.getReasonCode());
        detail.put("reasonMessage", this.resolveCarryoverReason(impact.getReasonCode()));
        return detail;
    }

    /** 将滚动限制原因转换为用户可理解的国际化说明。 */
    private String resolveCarryoverReason(String reasonCode) {
        if ("CAPACITY_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.capacityLimit");
        }
        if ("STORAGE_LANE_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.storageLaneLimit");
        }
        if ("ROLL_TOOL_LIMIT".equals(reasonCode) || "TOOLING_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.toolingLimit");
        }
        if ("BIG_ROLL_STOCK_DATA_MISSING".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.bigRollStockDataMissing");
        }
        if ("CONSTRUCTION_MISSING".equals(reasonCode) || "DATA_MISSING".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.constructionMissing");
        }
        if ("AGING_PERIOD_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.agingPeriodLimit");
        }
        if ("SCHEDULE_WINDOW_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.scheduleWindowLimit");
        }
        return I18nUtil.getMessage("ui.cd90.insert.reason.other");
    }

    /**
     * 查询插单异步任务状态。
     * 按任务ID查询，校验任务类型为 INSERT_ORDER 后返回任务详情。
     *
     * @param taskId 任务编号
     * @return 任务详情，任务不存在或类型不匹配时返回错误
     */
    @Override
    public AjaxResult getInsertTask(String taskId) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskType.INSERT_ORDER.equals(task.getTaskType())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.taskNotFound"));
        }
        return AjaxResult.success(task);
    }
    /**
     * 校验转机台请求是否合法。
     *
     * @param request 转机台请求
     * @return 校验结果
    */
    @Override
    public AjaxResult validateTransferMachine(Cd90TransferMachineRequest request) {
        if (request == null || request.getScheduleDate() == null
                || isBlank(request.getFactoryCode()) || isBlank(request.getSourceMachineCode())
                || isBlank(request.getTargetMachineCode()) || isBlank(request.getClothCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.required"));
        }
        if (request.getSourceMachineCode().equals(request.getTargetMachineCode())) {
            return AjaxResult.error("原机台和目标机台不能相同");
        }
        int editableFromClassIndex = this.resolveChangeQtyEditableFromClassIndex(
                request.getScheduleDate(), request.getFactoryCode());
        if (editableFromClassIndex > 6) {
            return AjaxResult.error("当前排程窗口已结束，不能转机台");
        }
        request.setStartClassField("CLASS" + editableFromClassIndex);
        List<Cd90ScheduleResult> existing = this.selectByDateAndFactory(
                request.getScheduleDate(), request.getFactoryCode());
        List<Cd90ScheduleResult> transferPlans = existing.stream()
                .filter(item -> request.getSourceMachineCode().equals(item.getMachineCode()))
                .filter(item -> request.getClothCode().equals(item.getClothCode()))
                .collect(Collectors.toList());
        if (transferPlans.isEmpty()) {
            return AjaxResult.error("原机台没有可转走的帘布计划");
        }
        boolean invalidClassOrder = IntStream.rangeClosed(1, editableFromClassIndex - 1)
                .anyMatch(classIndex -> readTransferProduceOrder(request, classIndex) != null);
        if (invalidClassOrder) {
            return AjaxResult.error("当前班次之前的数据不能转机台");
        }
        boolean zeroQuantitySelected = IntStream.rangeClosed(editableFromClassIndex, 6)
                .anyMatch(classIndex -> readTransferProduceOrder(request, classIndex) != null
                        && transferPlans.stream().mapToDouble(item -> readPlanQuantity(item, classIndex)).sum() <= 0D);
        if (zeroQuantitySelected) {
            return AjaxResult.error("计划量为0的班次不能转机台");
        }
        boolean missingProduceOrder = IntStream.rangeClosed(editableFromClassIndex, 6)
                .anyMatch(classIndex -> transferPlans.stream()
                        .mapToDouble(item -> readPlanQuantity(item, classIndex)).sum() > 0D
                        && readTransferProduceOrder(request, classIndex) == null);
        if (missingProduceOrder) {
            return AjaxResult.error("转机台目标顺序不能为空");
        }
        boolean hasTransferPlan = IntStream.rangeClosed(editableFromClassIndex, 6)
                .anyMatch(classIndex -> transferPlans.stream()
                        .mapToDouble(item -> readPlanQuantity(item, classIndex)).sum() > 0D);
        if (!hasTransferPlan) {
            return AjaxResult.error("当前班次及后续没有可转走的帘布计划");
        }
        boolean lockedTransfer = IntStream.rangeClosed(editableFromClassIndex, 6)
                .anyMatch(classIndex -> transferPlans.stream()
                        .anyMatch(item -> readPlanQuantity(item, classIndex) > 0D && isLocked(item, classIndex)));
        return lockedTransfer ? AjaxResult.error("已锁定或已生产的班次计划不能转机台") : AjaxResult.success();
    }
    /**
     * 提交转机台滚动重排任务。
     *
     * @param request 转机台请求
     * @return 提交结果，必要时返回跨班顺延确认信息
    */
    @Override
    public AjaxResult transferMachine(Cd90TransferMachineRequest request) {
        AjaxResult validation = this.validateTransferMachine(request);
        if (!Integer.valueOf(200).equals(validation.get("code"))) {
            return validation;
        }
        LocalDate localScheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Cd90BatchDataCheckResult batchCheck = batchDataValidator.check(
                request.getFactoryCode(), localScheduleDate);
        if (batchCheck.isFailed()) {
            Map<String, Object> data = new HashMap<>();
            data.put("batchCheckFailed", true);
            data.put("errors", toErrorList(batchCheck.getErrors()));
            data.put("warnings", toErrorList(batchCheck.getWarnings()));
            return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
        }
        Cd90ScheduleTask activeTask = taskService.findActive(
                request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
        }
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            AjaxResult previewResult = this.previewTransferMachine(request, localScheduleDate);
            if (previewResult != null) {
                return previewResult;
            }
        }
        Cd90ScheduleTask task = taskService.createPending(request.getFactoryCode(),
                request.getScheduleDate(), Cd90ScheduleTaskType.TRANSFER_MACHINE,
                "MANUAL", request.toString(), null);
        insertOrderAsyncExecutor.executeTransfer(task.getTaskId(), request);
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getTaskId());
        return AjaxResult.success("转机台任务已提交", data);
    }

    /**
     * 使用正式滚动内核执行转机台只读预演，跨班顺延时返回确认明细。
     * 获取分布式锁后执行转机台滚动排程，若产生跨班顺延影响则返回 needConfirm 结构，
     * 由前端展示顺延详情让用户确认后正式提交。
     *
     * @param request      转机台请求
     * @param scheduleDate 排程日期
     * @return 有跨班顺延时返回确认信息，无顺延时返回 null，获取锁失败时返回错误
     */
    private AjaxResult previewTransferMachine(Cd90TransferMachineRequest request,
                                              LocalDate scheduleDate) {
        RLock lock = lockService.getLock(request.getFactoryCode(), scheduleDate);
        try {
            if (!lock.tryLock()) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
            }
            if (taskService.findActive(request.getFactoryCode(), request.getScheduleDate()) != null) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
            }
            Cd90InsertRollingOutput output = insertRollingService.executeTransfer(request);
            List<Cd90InsertCarryoverImpact> impacts = output.getCarryoverImpacts() == null
                    ? Collections.emptyList() : output.getCarryoverImpacts();
            if (impacts.isEmpty()) {
                return null;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("needConfirm", true);
            data.put("carryoverDetails", impacts.stream()
                    .map(this::toCarryoverDetail)
                    .collect(Collectors.toList()));
            return AjaxResult.success("转机台会引起跨班顺延，请确认后继续", data);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询转机台异步任务状态。
     * 按任务ID查询，校验任务类型为 TRANSFER_MACHINE 后返回任务详情。
     *
     * @param taskId 任务编号
     * @return 任务详情，任务不存在或类型不匹配时返回错误
     */
    @Override
    public AjaxResult getTransferMachineTask(String taskId) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskType.TRANSFER_MACHINE.equals(task.getTaskType())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.taskNotFound"));
        }
        return AjaxResult.success(task);
    }
    /**
     * 校验调量请求是否合法。
     *
     * @param request 调量请求
     * @return 校验结果
    */
    @Override
    public AjaxResult validateChangeQty(Cd90ChangeQtyRequest request) {
        AjaxResult validation = this.validateChangeQtyBasic(request);
        if (!Integer.valueOf(200).equals(validation.get("code"))) {
            return validation;
        }
        LocalDate localScheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Cd90BatchDataCheckResult batchCheck = batchDataValidator.check(
                request.getFactoryCode(), localScheduleDate);
        if (batchCheck.isFailed()) {
            Map<String, Object> data = new HashMap<>();
            data.put("batchCheckFailed", true);
            data.put("errors", toErrorList(batchCheck.getErrors()));
            data.put("warnings", toErrorList(batchCheck.getWarnings()));
            return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
        }
        AjaxResult previewResult = this.previewChangeQty(request, localScheduleDate);
        return previewResult != null
                && !Integer.valueOf(200).equals(previewResult.get("code"))
                ? previewResult : AjaxResult.success();
    }

    /**
     * 校验调量请求的字段、目标记录、班次窗口及完成量。
     *
     * @param request 调量请求
     * @return 基础校验结果
     */
    private AjaxResult validateChangeQtyBasic(Cd90ChangeQtyRequest request) {
        if (request == null || request.getScheduleDate() == null
                || isBlank(request.getFactoryCode()) || isBlank(request.getMachineCode())
                || isBlank(request.getClothCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.required"));
        }
        Map<Integer, Double> targetQtyByClass;
        try {
            targetQtyByClass = this.resolveChangeQtyTargets(request);
        } catch (IllegalArgumentException exception) {
            return AjaxResult.error(exception.getMessage());
        }
        List<Cd90ScheduleResult> existing = this.latestBatchResults(this.selectByDateAndFactory(
                request.getScheduleDate(), request.getFactoryCode()));
        Optional<Cd90ScheduleResult> targetOptional = this.findChangeQtyTarget(request, existing);
        if (!targetOptional.isPresent()) {
            return AjaxResult.error("未找到可调量的直裁排程结果");
        }
        Cd90ScheduleResult target = targetOptional.get();
        boolean allSame = targetQtyByClass.entrySet().stream().allMatch(entry ->
                BigDecimal.valueOf(this.readPlanQuantity(target, entry.getKey()))
                        .compareTo(BigDecimal.valueOf(entry.getValue())) == 0);
        if (allSame) {
            return AjaxResult.error("调量目标计划量与原计划一致");
        }
        int editableFromClassIndex = this.resolveChangeQtyEditableFromClassIndex(
                request.getScheduleDate(), request.getFactoryCode());
        if (editableFromClassIndex > 6) {
            return AjaxResult.error("当前排程窗口已结束，不能调量");
        }
        Optional<Integer> beforeCurrentClass = targetQtyByClass.keySet().stream()
                .filter(classIndex -> classIndex < editableFromClassIndex)
                .findFirst();
        if (beforeCurrentClass.isPresent()) {
            return AjaxResult.error("当前班次之前不可调量");
        }

        Optional<Map.Entry<Integer, Double>> lockedClass = targetQtyByClass.entrySet().stream()
                .filter(entry -> this.isLocked(target, entry.getKey()))
                .findFirst();
        if (lockedClass.isPresent()) {
            return AjaxResult.error("已锁定或已生产的班次计划不能调量");
        }
        Optional<Map.Entry<Integer, Double>> lessThanFinish = targetQtyByClass.entrySet().stream()
                .filter(entry -> {
                    Double finishQty = this.readDouble(target, String.format("class%dFinishQty", entry.getKey()));
                    return finishQty != null && entry.getValue() < finishQty;
                }).findFirst();
        return lessThanFinish.isPresent()
                ? AjaxResult.error("调量目标不能小于已完成数量") : AjaxResult.success();
    }
    /**
     * 提交调量滚动重排任务。
     *
     * @param request 调量请求
     * @return 提交结果，必要时返回跨班顺延确认信息
    */
    @Override
    public AjaxResult changeQty(Cd90ChangeQtyRequest request) {
        AjaxResult validation = this.validateChangeQtyBasic(request);
        if (!Integer.valueOf(200).equals(validation.get("code"))) {
            return validation;
        }
        LocalDate localScheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Cd90BatchDataCheckResult batchCheck = batchDataValidator.check(
                request.getFactoryCode(), localScheduleDate);
        if (batchCheck.isFailed()) {
            Map<String, Object> data = new HashMap<>();
            data.put("batchCheckFailed", true);
            data.put("errors", toErrorList(batchCheck.getErrors()));
            data.put("warnings", toErrorList(batchCheck.getWarnings()));
            return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
        }
        Cd90ScheduleTask activeTask = taskService.findActive(
                request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
        }
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            AjaxResult previewResult = this.previewChangeQty(request, localScheduleDate);
            if (previewResult != null) {
                return previewResult;
            }
        }
        Cd90ScheduleTask task = taskService.createPending(request.getFactoryCode(),
                request.getScheduleDate(), Cd90ScheduleTaskType.CHANGE_QTY,
                "MANUAL", request.toString(), null);
        insertOrderAsyncExecutor.executeChangeQty(task.getTaskId(), request);
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getTaskId());
        return AjaxResult.success("调量滚动重排任务已提交", data);
    }

    private AjaxResult previewChangeQty(Cd90ChangeQtyRequest request,
                                        LocalDate scheduleDate) {
        RLock lock = lockService.getLock(request.getFactoryCode(), scheduleDate);
        try {
            if (!lock.tryLock()) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
            }
            if (taskService.findActive(request.getFactoryCode(), request.getScheduleDate()) != null) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
            }
            Cd90InsertRollingOutput output = insertRollingService.executeChangeQty(request);
            List<Cd90InsertCarryoverImpact> impacts = output.getCarryoverImpacts() == null
                    ? Collections.emptyList() : output.getCarryoverImpacts();
            if (impacts.isEmpty()) {
                return null;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("needConfirm", true);
            data.put("carryoverDetails", impacts.stream()
                    .map(this::toCarryoverDetail)
                    .collect(Collectors.toList()));
            return AjaxResult.success("调量会引起跨班顺延，请确认后继续", data);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询调量异步任务状态。
     * 按任务ID查询，校验任务类型为 CHANGE_QTY 后返回任务详情。
     *
     * @param taskId 任务编号
     * @return 任务详情，任务不存在或类型不匹配时返回错误
     */
    @Override
    public AjaxResult getChangeQtyTask(String taskId) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskType.CHANGE_QTY.equals(task.getTaskType())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.taskNotFound"));
        }
        return AjaxResult.success(task);
    }
    /**
     * 执行定时滚动校验。
     * 委托给 Cd90TimedRollingCheckService 执行具体的滚动校验逻辑。
     *
     * @param request 滚动校验请求
     * @return 校验结果
     */
    @Override
    public AjaxResult checkTimedRolling(Cd90RollingCheckRequest request) {
        return timedRollingCheckService.check(request);
    }
    /**
     * 查询定时滚动任务执行状态，并补充调整数量、未排入数量等衍生信息。
     *
     * @param taskId 任务编号
     * @return 任务详情
    */
    @Override
    public AjaxResult getTimedRollingTask(String taskId) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskType.ROLLING_SCHEDULE.equals(task.getTaskType())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.rolling.taskNotFound"));
        }
        Map<String, Object> response = objectMapper.convertValue(task, Map.class);
        String targetShiftCode = null;
        String inputVersion = null;
        if (!isBlank(task.getRequestSnapshot())) {
            try {
                JsonNode snapshot = objectMapper.readTree(task.getRequestSnapshot());
                inputVersion = snapshot.path("inputVersion").asText(null);
                targetShiftCode = snapshot.path("target")
                        .path("targetShiftCode").asText(null);
            } catch (Exception exception) {
                response.put("snapshotParseError", true);
            }
        }
        Number adjustedCount = rollingAdjustLogMapper.selectCount(
                new LambdaQueryWrapper<Cd90ScheduleRollingAdjustLog>()
                        .eq(Cd90ScheduleRollingAdjustLog::getTaskId,
                                task.getTaskId()));
        Number unscheduledCount = 0;
        if (!isBlank(task.getBatchNo())) {
            unscheduledCount = unscheduleResultMapper.selectCount(
                    new LambdaQueryWrapper<Cd90UnscheduleResult>()
                            .eq(Cd90UnscheduleResult::getFactoryCode, task.getFactoryCode())
                            .eq(Cd90UnscheduleResult::getScheduleDate, task.getScheduleDate())
                            .eq(Cd90UnscheduleResult::getBatchNo, task.getBatchNo()));
        }
        response.put("targetShiftCode", targetShiftCode);
        response.put("inputVersion", inputVersion);
        response.put("sourceBatchNo", task.getBatchNo());
        response.put("adjustedCount", adjustedCount);
        response.put("unscheduledCount", unscheduledCount);
        return AjaxResult.success(response);
    }
    /**
     * 判断字符串是否为空白。
     * null 或去除首尾空格后长度为0时返回 true。
     *
     * @param value 待判断的字符串
     * @return 是否为空白
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 判断指定班次是否已锁定。
     * 锁定条件满足以下任一：
     * 1. 实体级 isLocked 标志为1；
     * 2. 该班次已完成数量（finishQty）大于0；
     * 3. 生产状态为"1"（生产中）且完成数量小于计划数量。
     *
     * @param result     排程结果实体
     * @param classIndex 班次索引（1-6）
     * @return 是否已锁定
     */
    private boolean isLocked(Cd90ScheduleResult result, int classIndex) {
        Double finishQuantity = this.readDouble(result, String.format("class%dFinishQty", classIndex));
        Double planQuantity = this.readDouble(result, String.format("class%dPlanQty", classIndex));
        return Integer.valueOf(1).equals(result.getIsLocked())
                || (finishQuantity != null && finishQuantity > 0D)
                || ("1".equals(result.getProductionStatus())
                && planQuantity != null && (finishQuantity == null || finishQuantity < planQuantity));
    }

    /**
     * 读取指定班次的生产顺序值。
     *
     * @param result     排程结果实体
     * @param classIndex 班次索引（1-6）
     * @return 生产顺序值，未设置时返回 null
     */
    private Integer readProduceOrder(Cd90ScheduleResult result, int classIndex) {
        return (Integer) result.getFieldValueByFieldName(String.format(
                "class%dProduceOrder", classIndex));
    }

    /**
     * 读取指定班次的计划量。
     *
     * @param result     排程结果实体
     * @param classIndex 班次索引（1-6）
     * @return 计划量，未设置或为null时返回0
     */
    private double readPlanQuantity(Cd90ScheduleResult result, int classIndex) {
        Double value = this.readDouble(result, String.format("class%dPlanQty", classIndex));
        return value == null ? 0D : value;
    }

    /**
     * 解析调量请求中的目标计划量。
     * 支持两种方式传入调量目标：
     * 1. 通过 startClassField + targetPlanQty 指定单个班次的目标量；
     * 2. 通过 class1PlanQty~class6PlanQty 批量指定多个班次的目标量。
     * 两种方式可混合使用，最终合并为按班次索引映射的目标量集合。
     *
     * @param request 调量请求
     * @return 班次索引到目标计划量的映射
     * @throws IllegalArgumentException 调量参数不合法时抛出
     */
    private Map<Integer, Double> resolveChangeQtyTargets(Cd90ChangeQtyRequest request) {
        Map<Integer, Double> targetQtyByClass = new LinkedHashMap<>();
        if (!isBlank(request.getStartClassField()) || request.getTargetPlanQty() != null) {
            if (isBlank(request.getStartClassField()) || request.getTargetPlanQty() == null) {
                throw new IllegalArgumentException("调量班次和目标计划量必须同时填写");
            }
            int classIndex = this.parseClassIndex(request.getStartClassField());
            targetQtyByClass.put(classIndex, request.getTargetPlanQty());
        }
        IntStream.rangeClosed(1, 6).forEach(classIndex -> {
            Double planQty = (Double) request.getFieldValueByFieldName(
                    String.format("class%dPlanQty", classIndex));
            if (planQty != null) {
                targetQtyByClass.put(classIndex, planQty);
            }
        });
        if (targetQtyByClass.isEmpty()) {
            throw new IllegalArgumentException("至少填写一个调量目标计划量");
        }
        targetQtyByClass.forEach((classIndex, planQty) -> {
            if (classIndex < 1 || classIndex > 6 || planQty == null || planQty < 0D) {
                throw new IllegalArgumentException("调量班次必须为CLASS1至CLASS6，目标计划量不能小于0");
            }
        });
        return targetQtyByClass;
    }

    /**
     * 将班次字段名（如 CLASS1）解析为数字索引（1）。
     * 校验解析结果在1-6范围内，超出范围或格式错误时抛出 IllegalArgumentException。
     *
     * @param classField 班次字段名
     * @return 班次数字索引
     * @throws IllegalArgumentException 格式错误或超出范围时抛出
     */
    private int parseClassIndex(String classField) {
        try {
            int classIndex = Integer.parseInt(classField.replace("CLASS", ""));
            if (classIndex < 1 || classIndex > 6) {
                throw new NumberFormatException("class index out of range");
            }
            return classIndex;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("班次必须为CLASS1至CLASS6", exception);
        }
    }

    /**
     * 从排程结果列表中筛选最新批次的数据。
     * 取 batchNo 最大的记录作为最新批次，过滤出该批次的所有记录返回。
     * 用于调量时确保基于最新一次排程结果进行操作。
     *
     * @param results 排程结果列表
     * @return 最新批次的排程结果列表
     */
    private List<Cd90ScheduleResult> latestBatchResults(List<Cd90ScheduleResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        String latestBatchNo = results.stream().map(Cd90ScheduleResult::getBatchNo)
                .filter(Objects::nonNull).max(String::compareTo).orElse(null);
        return latestBatchNo == null ? Collections.emptyList() : results.stream()
                .filter(item -> latestBatchNo.equals(item.getBatchNo()))
                .collect(Collectors.toList());
    }

    /**
     * 查找调量目标排程结果。
     * 按排程结果ID（可选）、机台编码、帘布代号匹配目标记录。
     *
     * @param request  调量请求
     * @param existing 现有排程结果列表
     * @return 匹配的目标排程结果，未找到时返回空 Optional
     */
    private Optional<Cd90ScheduleResult> findChangeQtyTarget(Cd90ChangeQtyRequest request,
                                                             List<Cd90ScheduleResult> existing) {
        return existing.stream()
                .filter(item -> request.getScheduleResultId() == null
                        || Objects.equals(item.getId(), request.getScheduleResultId()))
                .filter(item -> request.getMachineCode().equals(item.getMachineCode()))
                .filter(item -> request.getClothCode().equals(item.getClothCode()))
                .findFirst();
    }
    /**
     * 读取转机台请求中指定班次的生产顺序值。
     * 仅当值大于0时返回，否则返回 null（视为未设置）。
     *
     * @param request    转机台请求
     * @param classIndex 班次索引（1-6）
     * @return 生产顺序值，未设置或为0时返回 null
     */
    private Integer readTransferProduceOrder(Cd90TransferMachineRequest request, int classIndex) {
        Integer produceOrder = (Integer) request.getFieldValueByFieldName(String.format(
                "class%dProduceOrder", classIndex));
        return produceOrder != null && produceOrder > 0 ? produceOrder : null;
    }

    /**
     * 读取排程结果中指定字段的 Double 值。
     * 通过动态字段名访问实体的 class1~class6 系列字段。
     *
     * @param result    排程结果实体
     * @param fieldName 字段名（如 class1PlanQty）
     * @return Double 值，未设置时返回 null
     */
    private Double readDouble(Cd90ScheduleResult result, String fieldName) {
        return (Double) result.getFieldValueByFieldName(fieldName);
    }

    /**
     * 获取文档类型编码。
     * 返回 CD90_SCHEDULE_RESULT 用于单据类型识别。
     *
     * @return 文档类型编码
     */
    @Override
    protected String getDocTypeCode() { return "CD90_SCHEDULE_RESULT"; }

    /**
     * 获取系统文档类型对象。
     * 构建包含 CD90_SCHEDULE_RESULT 编码的 SysDocType 实例。
     *
     * @return 系统文档类型对象
     */
    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_SCHEDULE_RESULT");
        return sysDocType;
    }

    /** 将批次级检查错误列表转为前端可渲染的List<Map>结构。 */
    private List<Map<String, Object>> toErrorList(List<Cd90BatchDataCheckResult.CheckError> errors) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (errors == null) {
            return result;
        }
        for (Cd90BatchDataCheckResult.CheckError error : errors) {
            Map<String, Object> item = new HashMap<>();
            item.put("field", error.getField());
            item.put("reasonCode", error.getReasonCode());
            item.put("message", error.getMessage());
            item.put("suggestion", error.getSuggestion());
            result.add(item);
        }
        return result;
    }

    /**
     * 检查插窗帘布的 TIRE_FABRIC_CRAFT 和 TIRE_FABRIC_LENGTH 施工数据。
     * 以帘布代号查询施工信息中匹配的层位，校验直裁宽度和单耗均存在且为正。
     *
     * @param factoryCode 工厂编码
     * @param clothCode   帘布代号
     * @return 失败时返回包含 batchCheckFailed 等的 Map（与前端的 batchCheckFailed=true 协定对齐），通过时返回 null
     */
    private Map<String, Object> checkInsertClothTireFabric(String factoryCode, String clothCode) {
        if (isBlank(clothCode)) {
            return null;
        }
        List<MdmConstructionInfo> constructions = constructionMapper.selectList(
                Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .and(w -> w.eq(MdmConstructionInfo::getTireFabricCode1, clothCode)
                                .or().eq(MdmConstructionInfo::getTireFabricCode2, clothCode)
                                .or().eq(MdmConstructionInfo::getTireFabricCode3, clothCode)));
        List<Map<String, Object>> errors = new ArrayList<>();
        boolean clothFound = false;
        if (constructions != null) {
            for (MdmConstructionInfo construction : constructions) {
                String prefix = "胎胚 " + construction.getConstructionCode()
                        + " 施工版本 " + construction.getConstructionVersion() + " ";
                for (int layer = 1; layer <= 3; layer++) {
                    String layerClothCode = getMdmLayerClothCode(construction, layer);
                    if (!clothCode.equals(layerClothCode)) {
                        continue;
                    }
                    clothFound = true;
                    // 检查 TIRE_FABRIC_CRAFT{n}
                    String craftRaw = getMdmLayerCraftRaw(construction, layer);
                    if (!isPositiveDecimal(craftRaw)) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("field", "施工信息");
                        error.put("reasonCode", "DATA_MISSING");
                        error.put("message", prefix + "第 " + layer + " 层帘布 " + clothCode + " 直裁宽度缺失或非正");
                        error.put("suggestion", "请在施工信息页面维护 TIRE_FABRIC_CRAFT" + layer + " 且大于0");
                        errors.add(error);
                    }
                    // 检查 TIRE_FABRIC_LENGTH{n}
                    BigDecimal length = getMdmLayerLength(construction, layer);
                    if (length == null || length.signum() <= 0) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("field", "施工信息");
                        error.put("reasonCode", "DATA_MISSING");
                        error.put("message", prefix + "第 " + layer + " 层帘布 " + clothCode + " 单耗缺失或非正");
                        error.put("suggestion", "请在施工信息页面维护 TIRE_FABRIC_LENGTH" + layer + " 且大于0");
                        errors.add(error);
                    }
                }
            }
        }
        if (!clothFound) {
            Map<String, Object> error = new HashMap<>();
            error.put("field", "施工信息");
            error.put("reasonCode", "DATA_MISSING");
            error.put("message", "帘布 " + clothCode + " 未在任何施工信息中找到");
            error.put("suggestion", "请检查帘布代号维护是否正确");
            errors.add(error);
        }
        if (!errors.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("batchCheckFailed", true);
            result.put("errors", errors);
            result.put("warnings", new ArrayList<>());
            return result;
        }
        return null;
    }

    /** 取施工记录指定层位的帘布代号（1=TIRE_FABRIC_CODE1, 2=TIRE_FABRIC_CODE2, 3=TIRE_FABRIC_CODE3）。 */
    private String getMdmLayerClothCode(MdmConstructionInfo construction, int layer) {
        switch (layer) {
            case 1: return construction.getTireFabricCode1();
            case 2: return construction.getTireFabricCode2();
            case 3: return construction.getTireFabricCode3();
            default: return null;
        }
    }

    /** 取施工记录指定层位的直裁宽度原始值（TIRE_FABRIC_CRAFT1/2/3）。 */
    private String getMdmLayerCraftRaw(MdmConstructionInfo construction, int layer) {
        switch (layer) {
            case 1: return construction.getTireFabricCraft1();
            case 2: return construction.getTireFabricCraft2();
            case 3: return construction.getTireFabricCraft3();
            default: return null;
        }
    }

    /** 取施工记录指定层位的单耗（TIRE_FABRIC_LENGTH1/2/3）。 */
    private BigDecimal getMdmLayerLength(MdmConstructionInfo construction, int layer) {
        switch (layer) {
            case 1: return construction.getTireFabricLength1();
            case 2: return construction.getTireFabricLength2();
            case 3: return construction.getTireFabricLength3();
            default: return null;
        }
    }

    /** 判断字符串是否为正数（可解析为 >0 的数值）。 */
    private boolean isPositiveDecimal(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        try {
            return new BigDecimal(raw.trim()).signum() > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    /**
     * 按排程日期和工厂查询直裁排程结果。
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  工厂编码
     * @return 排程结果列表，参数为空时返回空列表
     */
    @Override
    public List<Cd90ScheduleResult> selectByDateAndFactory(Date scheduleDate, String factoryCode) {
        if (scheduleDate == null || factoryCode == null || factoryCode.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Cd90ScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90ScheduleResult::getScheduleDate, scheduleDate)
                .eq(Cd90ScheduleResult::getFactoryCode, factoryCode);
        return cd90ScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 按ID列表批量查询排程结果。
     *
     * @param ids 排程结果ID列表
     * @return 匹配的排程结果列表，ID列表为空时返回空列表
     */
    @Override
    public List<Cd90ScheduleResult> getCd90ScheduleResultListByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Cd90ScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Cd90ScheduleResult::getId, ids);
        return cd90ScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 批量更新发布状态。REQUIRES_NEW 独立短事务：
     * 即便外层 MES 调用 try 块抛异常，失败状态回写也能独立提交，避免状态丢失。
     *
     * @param list 需要更新的排程结果集合
     * @param targetStatus 目标发布状态
     * @return 实际更新数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public int batchUpdateReleaseStatus(List<Cd90ScheduleResult> list, String targetStatus) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        Date now = new Date();
        for (Cd90ScheduleResult entity : list) {
            entity.setIsRelease(targetStatus);
            if (ApsConstant.IS_RELEASE.equals(targetStatus)) {
                entity.setPublishSuccessCount(
                        Optional.ofNullable(entity.getPublishSuccessCount()).orElse(0) + 1);
                entity.setNewestPublishTime(now);
            }
        }
        this.baseDao.updateBatch(list);
        return list.size();
    }
}
