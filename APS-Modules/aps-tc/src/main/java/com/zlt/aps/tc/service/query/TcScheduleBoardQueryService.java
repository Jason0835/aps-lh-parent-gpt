package com.zlt.aps.tc.service.query;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.*;
import com.zlt.aps.tc.api.domain.vo.*;
import com.zlt.aps.tc.api.enums.TcYesNoEnum;
import com.zlt.aps.tc.mapper.*;
import com.zlt.aps.tc.service.TcShiftStartTimeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧排程看板聚合查询服务。
 *
 * <p>看板列表只读取排程与班次基础字段，解释大字段由明细方法按需加载，避免列表 N+1 查询。</p>
 */
@Service
@Slf4j
public class TcScheduleBoardQueryService {

    private final TcScheduleResultMapper scheduleResultMapper;

    private final TcScheduleUnplannedMapper scheduleUnplannedMapper;

    private final TcScheduleResultExplainMapper scheduleResultExplainMapper;

    private final TcShiftConfigMapper shiftConfigMapper;

    private final TcParamsMapper paramsMapper;

    private final TcShiftStartTimeResolver shiftStartTimeResolver;

    /**
     * 构造胎侧排程看板查询服务。
     *
     * @param scheduleResultMapper 排程结果 Mapper
     * @param scheduleUnplannedMapper 未排任务 Mapper
     * @param scheduleResultExplainMapper 排程解释 Mapper
     * @param shiftConfigMapper 班次配置 Mapper
     * @param paramsMapper 胎侧参数 Mapper
     * @param shiftStartTimeResolver 班次开始时间解析服务
     */
    @Autowired
    public TcScheduleBoardQueryService(TcScheduleResultMapper scheduleResultMapper,
                                       TcScheduleUnplannedMapper scheduleUnplannedMapper,
                                       TcScheduleResultExplainMapper scheduleResultExplainMapper,
                                       TcShiftConfigMapper shiftConfigMapper,
                                       TcParamsMapper paramsMapper,
                                       TcShiftStartTimeResolver shiftStartTimeResolver) {
        this.scheduleResultMapper = scheduleResultMapper;
        this.scheduleUnplannedMapper = scheduleUnplannedMapper;
        this.scheduleResultExplainMapper = scheduleResultExplainMapper;
        this.shiftConfigMapper = shiftConfigMapper;
        this.paramsMapper = paramsMapper;
        this.shiftStartTimeResolver = shiftStartTimeResolver;
    }

    /**
     * 创建兼容旧单元测试和调用方的看板查询服务。
     *
     * @param scheduleResultMapper 排程结果 Mapper
     * @param scheduleUnplannedMapper 未排任务 Mapper
     * @param scheduleResultExplainMapper 排程解释 Mapper
     * @param shiftConfigMapper 班次配置 Mapper
     * @param paramsMapper 胎侧参数 Mapper
     */
    public TcScheduleBoardQueryService(TcScheduleResultMapper scheduleResultMapper,
                                       TcScheduleUnplannedMapper scheduleUnplannedMapper,
                                       TcScheduleResultExplainMapper scheduleResultExplainMapper,
                                       TcShiftConfigMapper shiftConfigMapper,
                                       TcParamsMapper paramsMapper) {
        this(scheduleResultMapper, scheduleUnplannedMapper, scheduleResultExplainMapper,
                shiftConfigMapper, paramsMapper, null);
    }

    /**
     * 分页查询排程看板及汇总信息。
     *
     * @param queryVo 看板查询条件
     * @return 排程看板聚合结果
     * @throws ServiceException 工厂或日期范围缺失、日期倒置时抛出
     */
    public TcScheduleBoardVo queryBoard(TcScheduleBoardQueryVo queryVo) {
        this.validateQuery(queryVo);
        List<TcScheduleResult> currentBatchResultList = this.loadCurrentBatchResultList(queryVo);
        List<TcScheduleUnplanned> currentBatchUnplannedList = this.loadCurrentBatchUnplannedList(queryVo);
        Map<String, String> batchMap = this.buildBatchMap(currentBatchResultList, currentBatchUnplannedList);
        boolean queryScheduled = !"UNPLANNED".equalsIgnoreCase(queryVo.getAssignStatus());
        List<TcScheduleResult> summaryResultList = queryScheduled && !batchMap.isEmpty()
                ? this.emptyIfNull(this.scheduleResultMapper.selectList(this.buildResultWrapper(queryVo, batchMap)))
                : Collections.emptyList();

        int pageNum = queryVo.getPageNum() == null || queryVo.getPageNum() < 1 ? 1 : queryVo.getPageNum();
        int pageSize = queryVo.getPageSize() == null || queryVo.getPageSize() < 1 ? 20 : queryVo.getPageSize();
        Page<TcScheduleResult> resultPage = queryScheduled && !batchMap.isEmpty()
                ? this.queryScheduledPage(queryVo, batchMap, pageNum, pageSize)
                : new Page<>(pageNum, pageSize);

        TcScheduleBoardVo boardVo = new TcScheduleBoardVo();
        boardVo.setScheduledPage(this.buildScheduledPage(resultPage, pageNum, pageSize));
        boardVo.setDateColumns(this.loadDateColumns(queryVo));
        boardVo.setBatchMap(batchMap);
        boardVo.setSummary(this.buildSummary(summaryResultList));
        boardVo.setUnplannedCount(this.countUnplanned(queryVo, boardVo.getBatchMap()));
        return boardVo;
    }

    /**
     * 查询已排结果的解释明细。
     *
     * @param resultId 排程结果 ID
     * @return 解释明细
     */
    public List<TcScheduleResultExplain> listResultExplain(Long resultId) {
        if (resultId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TcScheduleResultExplain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResultExplain::getResultId, resultId);
        wrapper.orderByAsc(TcScheduleResultExplain::getId);
        return this.emptyIfNull(this.scheduleResultExplainMapper.selectList(wrapper));
    }

    /**
     * 查询未排任务的解释明细。
     *
     * @param unplannedId 未排任务 ID
     * @return 解释明细
     */
    public List<TcScheduleResultExplain> listUnplannedExplain(Long unplannedId) {
        TcScheduleUnplanned unplanned = unplannedId == null ? null
                : this.scheduleUnplannedMapper.selectById(unplannedId);
        if (unplanned == null || StringUtils.isBlank(unplanned.getTaskBusinessKey())) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TcScheduleResultExplain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResultExplain::getBatchNo, unplanned.getBatchNo());
        wrapper.eq(TcScheduleResultExplain::getTaskBusinessKey, unplanned.getTaskBusinessKey());
        wrapper.orderByAsc(TcScheduleResultExplain::getId);
        return this.emptyIfNull(this.scheduleResultExplainMapper.selectList(wrapper));
    }

    /**
     * 分页查询当前有效批次的未排任务。
     *
     * @param queryVo 查询条件
     * @return 未排任务分页
     */
    public Page<TcScheduleUnplanned> listUnplanned(TcScheduleBoardQueryVo queryVo) {
        this.validateQuery(queryVo);
        int pageNum = queryVo.getPageNum() == null || queryVo.getPageNum() < 1 ? 1 : queryVo.getPageNum();
        int pageSize = queryVo.getPageSize() == null || queryVo.getPageSize() < 1 ? 20 : queryVo.getPageSize();
        if ("ASSIGNED".equalsIgnoreCase(queryVo.getAssignStatus())) {
            return new Page<>(pageNum, pageSize);
        }
        List<String> batchNoList = StringUtils.isNotBlank(queryVo.getBatchNo())
                ? Collections.singletonList(queryVo.getBatchNo())
                : new ArrayList<>(this.buildBatchMap(this.loadCurrentBatchResultList(queryVo),
                this.loadCurrentBatchUnplannedList(queryVo)).values());
        if (batchNoList.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }
        LambdaQueryWrapper<TcScheduleUnplanned> wrapper = this.buildUnplannedWrapper(queryVo, batchNoList);
        wrapper.orderByAsc(TcScheduleUnplanned::getScheduleDate, TcScheduleUnplanned::getShiftOrder,
                TcScheduleUnplanned::getSidewallCode);
        return this.queryUnplannedPage(wrapper, pageNum, pageSize);
    }

    /**
     * 分页查询胎侧已排结果。
     *
     * <p>与参数列表保持一致，使用 PageHelper 完成分页和总数统计，再转换为现有 Feign 分页契约。</p>
     *
     * @param queryVo 查询条件
     * @param batchMap 排程日期与当前有效批次映射
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 已排结果分页
     */
    private Page<TcScheduleResult> queryScheduledPage(TcScheduleBoardQueryVo queryVo,
                                                      Map<String, String> batchMap,
                                                      int pageNum,
                                                      int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        try {
            List<TcScheduleResult> records = this.emptyIfNull(
                    this.scheduleResultMapper.selectList(this.buildResultWrapper(queryVo, batchMap)));
            PageInfo<TcScheduleResult> pageInfo = new PageInfo<>(records);
            Page<TcScheduleResult> resultPage = new Page<>(pageNum, pageSize, pageInfo.getTotal());
            resultPage.setRecords(records);
            return resultPage;
        } finally {
            PageHelper.clearPage();
        }
    }

    /**
     * 分页查询胎侧未排任务。
     *
     * <p>使用 PageHelper 统计真实总数，避免重新打开未排任务页面时响应 records 有数据但 total 为 0。</p>
     *
     * @param wrapper 未排任务查询条件
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 未排任务分页
     */
    private Page<TcScheduleUnplanned> queryUnplannedPage(LambdaQueryWrapper<TcScheduleUnplanned> wrapper,
                                                         int pageNum,
                                                         int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        try {
            List<TcScheduleUnplanned> records = this.emptyIfNull(
                    this.scheduleUnplannedMapper.selectList(wrapper));
            PageInfo<TcScheduleUnplanned> pageInfo = new PageInfo<>(records);
            Page<TcScheduleUnplanned> resultPage = new Page<>(pageNum, pageSize, pageInfo.getTotal());
            resultPage.setRecords(records);
            return resultPage;
        } finally {
            PageHelper.clearPage();
        }
    }

    /**
     * 按工厂和日期范围加载当前有效批次行。
     *
     * <p>批次恢复不能受胎侧、胶料、口型或机台等看板行筛选影响，否则筛选无命中时会错误丢失未排任务。</p>
     *
     * @param queryVo 看板查询条件
     * @return 当前有效结果行
     */
    private List<TcScheduleResult> loadCurrentBatchResultList(TcScheduleBoardQueryVo queryVo) {
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, queryVo.getFactoryCode());
        wrapper.ge(TcScheduleResult::getScheduleDate, queryVo.getStartDate());
        wrapper.le(TcScheduleResult::getScheduleDate, queryVo.getEndDate());
        wrapper.orderByAsc(TcScheduleResult::getScheduleDate, TcScheduleResult::getBatchNo);
        return this.emptyIfNull(this.scheduleResultMapper.selectList(wrapper));
    }

    /**
     * 按工厂和日期范围加载当前有效批次未排行。
     *
     * <p>当某次自动排程全部任务均未排时结果表没有记录，必须由未排表补足当前批次，
     * 否则看板无法恢复该批次的未排数量和未排列表。</p>
     *
     * @param queryVo 看板查询条件
     * @return 当前有效未排任务
     */
    private List<TcScheduleUnplanned> loadCurrentBatchUnplannedList(TcScheduleBoardQueryVo queryVo) {
        LambdaQueryWrapper<TcScheduleUnplanned> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleUnplanned::getFactoryCode, queryVo.getFactoryCode());
        wrapper.ge(TcScheduleUnplanned::getScheduleDate, queryVo.getStartDate());
        wrapper.le(TcScheduleUnplanned::getScheduleDate, queryVo.getEndDate());
        wrapper.gt(TcScheduleUnplanned::getPlanQty, BigDecimal.ZERO);
        wrapper.orderByAsc(TcScheduleUnplanned::getScheduleDate, TcScheduleUnplanned::getBatchNo);
        return this.emptyIfNull(this.scheduleUnplannedMapper.selectList(wrapper));
    }

    /**
     * 构造排程结果查询条件。
     *
     * @param queryVo 查询条件
     * @return 排程结果 Lambda 查询条件
     */
    private LambdaQueryWrapper<TcScheduleResult> buildResultWrapper(TcScheduleBoardQueryVo queryVo,
                                                                     Map<String, String> batchMap) {
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, queryVo.getFactoryCode());
        wrapper.ge(TcScheduleResult::getScheduleDate, queryVo.getStartDate());
        wrapper.le(TcScheduleResult::getScheduleDate, queryVo.getEndDate());
        wrapper.eq(StringUtils.isNotBlank(queryVo.getMachineCode()), TcScheduleResult::getMachineCode,
                queryVo.getMachineCode());
        wrapper.eq(StringUtils.isNotBlank(queryVo.getSidewallCode()), TcScheduleResult::getSidewallCode,
                queryVo.getSidewallCode());
        wrapper.eq(StringUtils.isNotBlank(queryVo.getGlueCode()), TcScheduleResult::getGlueCode,
                queryVo.getGlueCode());
        wrapper.eq(StringUtils.isNotBlank(queryVo.getMouthPlateCode()), TcScheduleResult::getMouthPlateCode,
                queryVo.getMouthPlateCode());
        wrapper.eq(StringUtils.isNotBlank(queryVo.getReleaseStatus()), TcScheduleResult::getReleaseStatus,
                queryVo.getReleaseStatus());
        this.applyCurrentBatchScope(wrapper, batchMap);
        wrapper.orderByAsc(TcScheduleResult::getScheduleDate, TcScheduleResult::getMachineCode,
                TcScheduleResult::getOrderNo, TcScheduleResult::getId);
        return wrapper;
    }

    /**
     * 将日期与当前批次的精确对应关系追加到结果查询，避免历史有效批次混入分页和汇总。
     *
     * @param wrapper 结果查询条件
     * @param batchMap 日期当前批次映射
     */
    private void applyCurrentBatchScope(LambdaQueryWrapper<TcScheduleResult> wrapper,
                                        Map<String, String> batchMap) {
        List<Map.Entry<String, String>> batchEntryList = batchMap.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue()))
                .collect(Collectors.toList());
        wrapper.and(scope -> {
            boolean first = true;
            for (Map.Entry<String, String> entry : batchEntryList) {
                Date scheduleDate = DateUtil.parseDate(entry.getKey());
                if (first) {
                    scope.eq(TcScheduleResult::getScheduleDate, scheduleDate)
                            .eq(TcScheduleResult::getBatchNo, entry.getValue());
                    first = false;
                } else {
                    scope.or(condition -> condition.eq(TcScheduleResult::getScheduleDate, scheduleDate)
                            .eq(TcScheduleResult::getBatchNo, entry.getValue()));
                }
            }
        });
    }

    /**
     * 加载看板日期班次列。
     *
     * @param queryVo 查询条件
     * @return 日期班次列
     */
    private List<TcScheduleBoardDateColumnVo> loadDateColumns(TcScheduleBoardQueryVo queryVo) {
        LambdaQueryWrapper<TcShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcShiftConfig::getFactoryCode, queryVo.getFactoryCode());
        wrapper.orderByAsc(TcShiftConfig::getShiftOrder);
        List<TcShiftConfig> shiftConfigList = this.emptyIfNull(this.shiftConfigMapper.selectList(wrapper));
        int shiftDateStartOffset = this.resolveShiftDateStartOffset(queryVo.getFactoryCode());
        List<TcScheduleBoardDateColumnVo> columnList = new ArrayList<>();
        Date currentDate = DateUtil.beginOfDay(queryVo.getStartDate());
        Date endDate = DateUtil.beginOfDay(queryVo.getEndDate());
        while (!currentDate.after(endDate)) {
            Date scheduleDate = currentDate;
            Map<Integer, Date> shiftStartTimeMap = this.shiftStartTimeResolver == null
                    ? Collections.emptyMap()
                    : this.shiftStartTimeResolver.resolveShiftStartTimes(queryVo.getFactoryCode(), scheduleDate);
            shiftConfigList.forEach(shiftConfig -> columnList.add(
                    this.buildDateColumn(scheduleDate, shiftConfig, shiftDateStartOffset, shiftStartTimeMap)));
            currentDate = DateUtil.offsetDay(currentDate, 1);
        }
        return columnList;
    }

    /**
     * 转换班次列对象。
     *
     * @param scheduleDate 排程日期
     * @param shiftConfig 班次配置
     * @param shiftDateStartOffset 一班相对排程日期的偏移天数
     * @param shiftStartTimeMap 班次实际开始时间
     * @return 日期班次列
     */
    private TcScheduleBoardDateColumnVo buildDateColumn(Date scheduleDate, TcShiftConfig shiftConfig,
                                                         int shiftDateStartOffset,
                                                         Map<Integer, Date> shiftStartTimeMap) {
        TcScheduleBoardDateColumnVo columnVo = new TcScheduleBoardDateColumnVo();
        Date shiftStartTime = shiftStartTimeMap.get(shiftConfig.getShiftOrder());
        columnVo.setScheduleDate(shiftStartTime == null
                ? this.resolveShiftScheduleDate(scheduleDate, shiftConfig.getShiftOrder(), shiftDateStartOffset)
                : DateUtil.beginOfDay(shiftStartTime));
        columnVo.setShiftStartTime(shiftStartTime);
        columnVo.setShiftOrder(shiftConfig.getShiftOrder());
        columnVo.setShiftCode(shiftConfig.getShiftCode());
        columnVo.setShiftName(shiftConfig.getShiftName());
        columnVo.setOpenFlag(shiftConfig.getOpenFlag());
        return columnVo;
    }

    /**
     * 根据排程日期和班次顺序计算班次实际生产日期。
     * 胎侧六班日期映射与胎面一致：1班使用参数定义的起始日期，
     * 2~4班在起始日期基础上加1天，5~6班加2天。
     *
     * @param scheduleDate 排程日期
     * @param shiftOrder 班次顺序
     * @param shiftDateStartOffset 一班相对排程日期的偏移天数
     * @return 班次实际生产日期
     */
    private Date resolveShiftScheduleDate(Date scheduleDate, Integer shiftOrder, int shiftDateStartOffset) {
        if (shiftOrder == null) {
            return scheduleDate;
        }
        int shiftWindowDayOffset = shiftOrder == 1 ? 0 : (shiftOrder >= 5 ? 2 : 1);
        return DateUtil.offsetDay(scheduleDate, shiftDateStartOffset + shiftWindowDayOffset);
    }

    /**
     * 读取一班相对排程日期的偏移天数。
     * 参数未维护、未启用、为空或不是整数时使用兼容旧逻辑的默认值。
     *
     * @param factoryCode 工厂编码
     * @return 一班相对排程日期的偏移天数
     */
    private int resolveShiftDateStartOffset(String factoryCode) {
        LambdaQueryWrapper<TcParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcParams::getFactoryCode, factoryCode);
        wrapper.eq(TcParams::getParamCode, TcScheduleConstants.PARAM_SHIFT_DATE_START_OFFSET);
        wrapper.eq(TcParams::getEnableStatus, TcYesNoEnum.YES.getCode());
        TcParams params = this.paramsMapper.selectOne(wrapper);
        if (params == null) {
            return TcScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
        String effectiveValue = StringUtils.isNotBlank(params.getParamValue())
                ? params.getParamValue() : params.getDefaultValue();
        if (StringUtils.isBlank(effectiveValue)) {
            return TcScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
        try {
            return Integer.parseInt(effectiveValue.trim());
        } catch (NumberFormatException exception) {
            log.warn("胎侧班次表头日期偏移参数格式错误，factoryCode={}, paramCode={}, paramValue={}",
                    factoryCode, TcScheduleConstants.PARAM_SHIFT_DATE_START_OFFSET, effectiveValue);
            return TcScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
    }

    /**
     * 构造按日期取当前批次的映射。
     *
     * @param resultList 已排结果
     * @param unplannedList 未排任务
     * @return 日期批次映射
     */
    private Map<String, String> buildBatchMap(List<TcScheduleResult> resultList,
                                               List<TcScheduleUnplanned> unplannedList) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, String> batchMap = new LinkedHashMap<>();
        resultList.stream().filter(item -> item.getScheduleDate() != null && StringUtils.isNotBlank(item.getBatchNo()))
                .sorted((left, right) -> Objects.toString(left.getBatchNo(), "")
                        .compareTo(Objects.toString(right.getBatchNo(), "")))
                .forEach(item -> batchMap.put(dateFormat.format(item.getScheduleDate()), item.getBatchNo()));
        unplannedList.stream().filter(item -> item.getScheduleDate() != null
                        && StringUtils.isNotBlank(item.getBatchNo()))
                .sorted((left, right) -> Objects.toString(left.getBatchNo(), "")
                        .compareTo(Objects.toString(right.getBatchNo(), "")))
                .forEach(item -> {
                    String scheduleDate = dateFormat.format(item.getScheduleDate());
                    String currentBatchNo = batchMap.get(scheduleDate);
                    if (StringUtils.isBlank(currentBatchNo) || item.getBatchNo().compareTo(currentBatchNo) > 0) {
                        batchMap.put(scheduleDate, item.getBatchNo());
                    }
                });
        return batchMap;
    }

    /**
     * 汇总六班计划量、完成量、库存及各班次计划量。
     *
     * @param resultList 排程结果
     * @return 看板汇总
     */
    private TcScheduleBoardSummaryVo buildSummary(List<TcScheduleResult> resultList) {
        TcScheduleBoardSummaryVo summaryVo = new TcScheduleBoardSummaryVo();
        BigDecimal totalStockQty = BigDecimal.ZERO;
        BigDecimal totalPlanQty = BigDecimal.ZERO;
        BigDecimal totalFinishQty = BigDecimal.ZERO;
        // 各班次计划量合计，下标 0 对应 1 班，长度固定为最大班次序号
        List<BigDecimal> shiftPlanQtyList = new ArrayList<>(
                Collections.nCopies(TcScheduleConstants.TC_MAX_SHIFT_ORDER, BigDecimal.ZERO));
        for (TcScheduleResult result : resultList) {
            totalStockQty = totalStockQty.add(BigDecimalUtils.valueOf(result.getStockQty()));
            for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
                BigDecimal shiftPlanQty = this.readDecimal(result,
                        String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
                totalPlanQty = totalPlanQty.add(shiftPlanQty);
                totalFinishQty = totalFinishQty.add(this.readDecimal(result,
                        String.format(TcScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder)));
                shiftPlanQtyList.set(shiftOrder - 1, shiftPlanQtyList.get(shiftOrder - 1).add(shiftPlanQty));
            }
        }
        summaryVo.setTotalStockQty(totalStockQty);
        summaryVo.setTotalPlanQty(totalPlanQty);
        summaryVo.setTotalFinishQty(totalFinishQty);
        summaryVo.setResultCount((long) resultList.size());
        summaryVo.setShiftPlanQtyList(shiftPlanQtyList);
        return summaryVo;
    }

    /**
     * 统计当前有效批次未排任务数。
     *
     * @param queryVo 查询条件
     * @param batchMap 日期批次映射
     * @return 未排任务数
     */
    private Long countUnplanned(TcScheduleBoardQueryVo queryVo, Map<String, String> batchMap) {
        List<String> batchNoList = batchMap.values().stream().filter(StringUtils::isNotBlank).distinct()
                .collect(Collectors.toList());
        if (batchNoList.isEmpty()) {
            return 0L;
        }
        Number count = this.scheduleUnplannedMapper.selectCount(this.buildUnplannedWrapper(queryVo, batchNoList));
        return count == null ? 0L : count.longValue();
    }

    /**
     * 构造未排任务查询条件。
     *
     * @param queryVo 看板查询条件
     * @param batchNoList 当前批次号
     * @return 未排任务查询条件
     */
    private LambdaQueryWrapper<TcScheduleUnplanned> buildUnplannedWrapper(TcScheduleBoardQueryVo queryVo,
                                                                           List<String> batchNoList) {
        LambdaQueryWrapper<TcScheduleUnplanned> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleUnplanned::getFactoryCode, queryVo.getFactoryCode());
        wrapper.ge(TcScheduleUnplanned::getScheduleDate, queryVo.getStartDate());
        wrapper.le(TcScheduleUnplanned::getScheduleDate, queryVo.getEndDate());
        wrapper.in(TcScheduleUnplanned::getBatchNo, batchNoList);
        wrapper.gt(TcScheduleUnplanned::getPlanQty, BigDecimal.ZERO);
        wrapper.eq(StringUtils.isNotBlank(queryVo.getSidewallCode()), TcScheduleUnplanned::getSidewallCode,
                queryVo.getSidewallCode());
        wrapper.eq(StringUtils.isNotBlank(queryVo.getGlueCode()), TcScheduleUnplanned::getGlueCode,
                queryVo.getGlueCode());
        wrapper.eq(StringUtils.isNotBlank(queryVo.getMouthPlateCode()), TcScheduleUnplanned::getMouthPlateCode,
                queryVo.getMouthPlateCode());
        return wrapper;
    }

    /**
     * 构造分页返回对象。
     *
     * @param resultPage MyBatis 分页结果
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 看板分页对象
     */
    private TcScheduleBoardPageVo buildScheduledPage(Page<TcScheduleResult> resultPage, int pageNum, int pageSize) {
        TcScheduleBoardPageVo pageVo = new TcScheduleBoardPageVo();
        if (resultPage != null) {
            List<TcScheduleResult> resultList = resultPage.getRecords() == null
                    ? Collections.emptyList() : resultPage.getRecords();
            resultList.forEach(item -> item.setCurrentTaskVersion(item.getTaskVersion()));
            pageVo.setRows(resultList);
            pageVo.setTotal(resultPage.getTotal());
        }
        pageVo.setPageNum(pageNum);
        pageVo.setPageSize(pageSize);
        return pageVo;
    }

    /**
     * 校验看板查询条件。
     *
     * @param queryVo 查询条件
     * @throws ServiceException 工厂、日期缺失或日期倒置时抛出
     */
    private void validateQuery(TcScheduleBoardQueryVo queryVo) {
        if (queryVo == null || StringUtils.isBlank(queryVo.getFactoryCode())
                || queryVo.getStartDate() == null || queryVo.getEndDate() == null
                || queryVo.getStartDate().after(queryVo.getEndDate())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.board.invalidQuery"));
        }
    }

    /**
     * 将动态字段值转换为 BigDecimal。
     *
     * @param result 排程结果
     * @param fieldName 字段名
     * @return 非空数值
     */
    private BigDecimal readDecimal(TcScheduleResult result, String fieldName) {
        Object fieldValue = result.getFieldValueByFieldName(fieldName);
        return fieldValue instanceof BigDecimal ? (BigDecimal) fieldValue : BigDecimal.ZERO;
    }

    /**
     * 将可能为空的列表标准化为空列表。
     *
     * @param source 原列表
     * @param <T> 元素类型
     * @return 非空列表
     */
    private <T> List<T> emptyIfNull(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }
}
