package com.zlt.aps.lh.service;

import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 硫化日模具/机台数计算服务
 * <p>封装 {@link com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine#getMouldByDay} 的调用，
 * 复用 aps-lh 上下文中已有的月计划、工作日历数据，补充加载模具计算所需排产参数。</p>
 *
 * @author APS
 */
public interface ILhDailyMouldCalcService {

    /**
     * 从工作日历构建日产能限制Map并合并放入上下文（支持跨月，多次调用按年月累积）
     * <p>按指定年月过滤上下文中的工作日历列表，逐日构建 {@link com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo}，
     * 设置日产比例和开产首日标识，结果以日期(LocalDate)为key合并存入
     * {@code context.dailyCapacityLimitVoMap}（跨月日期不冲突）。</p>
     *
     * @param context 排程上下文（需已加载 workCalendarList）
     * @param year    目标年份
     * @param month   目标月份
     */
    void loadDailyCapacityLimitMap(LhScheduleContext context, int year, int month);

    /**
     * 从 T_MP_FACTORY_PARAM 加载模具计算所需排产参数并放入上下文
     * <p>加载 MonthPlanEnums 中的 SYS0203003~SYS0203006 四个参数，
     * 结果存入 {@code context.mouldAdjustParamMap}。</p>
     *
     * @param context     排程上下文
     * @param factoryCode 工厂编码
     * @return 模具计算排产参数Map
     */
    Map<String, Object> loadMouldAdjustParamMap(LhScheduleContext context, String factoryCode);

    /**
     * 获取单个SKU在指定日期的模具数和机台数（支持跨月，date 所在月需已加载日产能限制Map）
     *
     * @param context        排程上下文（需已加载 dailyCapacityLimitVoMap 和 mouldAdjustParamMap）
     * @param monthPlanEntity date 所在月的月计划定稿实体（跨月时为对应月的计划行）
     * @param date           目标日期（窗口内任意一天，可为跨月日期）
     * @return 模具计算结果，mouldQty=模具数，machineQty=机台数(=模具数/2)
     */
    DailyMouldResult getDailyMouldByDay(LhScheduleContext context,
                                        FactoryMonthPlanProductionFinalResult monthPlanEntity,
                                        Date date);

    /**
     * 按排程日期获取所有在产SKU的模具数汇总（单日口径，排程当月）
     * <p>从上下文月计划列表取排程当月记录，逐SKU调用 {@link #getDailyMouldByDay} 后汇总。</p>
     *
     * @param context      排程上下文
     * @param scheduleDate 排程日期（如 2026-08-21）
     * @return 各SKU模具计算结果列表
     */
    List<DailyMouldResult> getDailyMouldByDate(LhScheduleContext context, Date scheduleDate);

    /**
     * 预计算排程窗口及跨窗口判断范围内所有在产SKU每天的模具数/机台数并放入上下文（支持跨月）
     * <p>在数据加载环节调用，结果以 {@code materialCode|productStatus} 为key、
     * {@link DailyMouldSummary}（含停产保机前看、T～窗口结束日、T+3及特殊材料后看结果）为value存入
     * {@code context.dailyMouldResultMap}，供后续统一查询目标总机台数。</p>
     * <p>跨月处理：月计划行直接复用上下文 {@code monthPlanByMaterialMonthMap}
     * （loadMonthPlan 已按排程窗口所需月份批量加载当月+跨月计划行），不重复查库；
     * 某月无对应SKU计划行时，该SKU该日机台数记为0。</p>
     *
     * @param context 排程上下文（需已加载 workCalendarList、monthPlanList、monthPlanByMaterialMonthMap、scheduleDate）
     */
    void loadDailyMouldSummary(LhScheduleContext context);

    /**
     * 获取物料指定自然日所需的总硫化机台数。
     * <p>这是续作降模、续作加机台、新增排产和特殊材料置换的唯一目标机台数数据源；
     * 调用方只允许将该目标总数与当前有效在机数比较，不得再次根据日计划量或班产推算。</p>
     *
     * @param context       排程上下文
     * @param materialCode  物料编码
     * @param productStatus 产品状态
     * @param productionDate 目标自然日
     * @return 该物料+产品状态在指定日期所需的总硫化机台数；维度缺失时返回0
     */
    int getRequiredMachineCount(LhScheduleContext context,
                                String materialCode,
                                String productStatus,
                                LocalDate productionDate);

    /**
     * 判断统一计算结果Map中是否存在完整的物料、产品状态和自然日维度。
     * <p>用于区分“业务目标确实为0”和“初始化结果缺失”。降模等释放型决策在结果缺失时应保持现状，
     * 避免把缺失数据误当成目标0而释放全部在机机台。</p>
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @param productionDate 目标自然日
     * @return true-结果存在；false-维度或结果缺失
     */
    boolean hasRequiredMachineCount(LhScheduleContext context,
                                    String materialCode,
                                    String productStatus,
                                    LocalDate productionDate);

    /**
     * 从上下文中按物料编码+产品状态获取跨窗口模具汇总结果
     *
     * @param context       排程上下文
     * @param materialCode  物料编码
     * @param productStatus 产品状态
     * @return 窗口汇总结果（未找到返回null）
     */
    DailyMouldSummary getDailyMouldSummary(LhScheduleContext context, String materialCode, String productStatus);

    /**
     * 从上下文中按物料编码+产品状态获取指定日期的模具计算结果
     * <p>需先调用 {@link #loadDailyMouldSummary} 预计算。</p>
     *
     * @param context       排程上下文
     * @param materialCode  物料编码
     * @param productStatus 产品状态
     * @param productionDate 目标自然日
     * @return 指定日期的模具计算结果（未找到返回null）
     */
    DailyMouldResult getDailyMouldResult(LhScheduleContext context,
                                         String materialCode,
                                         String productStatus,
                                         LocalDate productionDate);

    /**
     * 日模具计算结果（单SKU单日）
     */
    class DailyMouldResult {
        /** 物料编码 */
        private String materialCode;
        /** 产品状态（如试制、常规等） */
        private String productStatus;
        /** 结构名称 */
        private String structureName;
        /** 对应自然日（排程窗口或跨窗口判断范围内某一天） */
        private LocalDate date;
        /** 月内天序号 */
        private Integer day;
        /** 模具数（getMouldByDay 返回值） */
        private int mouldQty;
        /** 机台数（=模具数/2） */
        private int machineQty;

        public DailyMouldResult() {
        }

        public DailyMouldResult(String materialCode, String productStatus, String structureName,
                                Integer day, int mouldQty) {
            this.materialCode = materialCode;
            this.productStatus = productStatus;
            this.structureName = structureName;
            this.day = day;
            this.mouldQty = mouldQty;
            this.machineQty = mouldQty / 2;
        }

        public String getMaterialCode() {
            return materialCode;
        }

        public void setMaterialCode(String materialCode) {
            this.materialCode = materialCode;
        }

        public String getProductStatus() {
            return productStatus;
        }

        public void setProductStatus(String productStatus) {
            this.productStatus = productStatus;
        }

        public String getStructureName() {
            return structureName;
        }

        public void setStructureName(String structureName) {
            this.structureName = structureName;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public Integer getDay() {
            return day;
        }

        public void setDay(Integer day) {
            this.day = day;
        }

        public int getMouldQty() {
            return mouldQty;
        }

        public void setMouldQty(int mouldQty) {
            this.mouldQty = mouldQty;
            // 机台数=模具数/2，同步更新，避免单独set后两个字段不一致
            this.machineQty = mouldQty / 2;
        }

        public int getMachineQty() {
            return machineQty;
        }

        public void setMachineQty(int machineQty) {
            this.machineQty = machineQty;
        }

        /**
         * 构建缓存Map的key：物料编码|产品状态
         *
         * @return 缓存key
         */
        public String buildCacheKey() {
            return buildCacheKey(materialCode, productStatus);
        }

        /**
         * 构建缓存Map的key：物料编码|产品状态
         *
         * @param materialCode 物料编码
         * @param productStatus 产品状态
         * @return 缓存key
         */
        public static String buildCacheKey(String materialCode, String productStatus) {
            return StringUtils.trimToEmpty(materialCode)
                    + "|" + StringUtils.trimToEmpty(productStatus);
        }
    }

    /**
     * 单SKU的日模具跨窗口汇总结果
     * <p>作为 {@code context.dailyMouldResultMap} 的value，
     * key 由 {@link DailyMouldResult#buildCacheKey(String, String)} 构建。</p>
     */
    class DailyMouldSummary {
        /** 物料编码 */
        private String materialCode;
        /** 产品状态（如试制、常规等） */
        private String productStatus;
        /** 结构名称 */
        private String structureName;
        /** 逐日模具结果, key=自然日, 按计算范围升序 */
        private final Map<LocalDate, DailyMouldResult> dayMouldMap = new java.util.LinkedHashMap<>();

        public DailyMouldSummary() {
        }

        public DailyMouldSummary(String materialCode, String productStatus, String structureName) {
            this.materialCode = materialCode;
            this.productStatus = productStatus;
            this.structureName = structureName;
        }

        /**
         * 放入某日的模具计算结果
         *
         * @param date   日期
         * @param result 该日模具计算结果
         */
        public void putDayMould(LocalDate date, DailyMouldResult result) {
            this.dayMouldMap.put(date, result);
        }

        /**
         * 获取某日的模具计算结果
         *
         * @param date 日期
         * @return 该日模具计算结果（未找到返回null）
         */
        public DailyMouldResult getDayMould(LocalDate date) {
            return this.dayMouldMap.get(date);
        }

        /**
         * 获取某日的机台数
         *
         * @param date 日期
         * @return 机台数（未找到返回0）
         */
        public int getDayMachineQty(LocalDate date) {
            DailyMouldResult result = this.dayMouldMap.get(date);
            return result == null ? 0 : result.getMachineQty();
        }

        /**
         * 获取窗口内逐日模具结果Map
         *
         * @return key=日期, value=模具计算结果
         */
        public Map<LocalDate, DailyMouldResult> getDayMouldMap() {
            return dayMouldMap;
        }

        public String getMaterialCode() {
            return materialCode;
        }

        public void setMaterialCode(String materialCode) {
            this.materialCode = materialCode;
        }

        public String getProductStatus() {
            return productStatus;
        }

        public void setProductStatus(String productStatus) {
            this.productStatus = productStatus;
        }

        public String getStructureName() {
            return structureName;
        }

        public void setStructureName(String structureName) {
            this.structureName = structureName;
        }
    }
}
