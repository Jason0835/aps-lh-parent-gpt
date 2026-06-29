package com.zlt.aps.tq.engine.handler;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.mapper.TqEngineMapper;
import com.zlt.aps.tq.engine.service.ITqDataLoadService;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * S1: 前置校验与数据加载Handler。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>通过 ITqDataLoadService 加载全部基础数据到Context</li>
 *   <li>校验施工信息完整性（胎胚代码、版本、胎圈代码、钢丝圈、三角胶、胶料、口型板、尺寸）</li>
 *   <li>为每条排程记录设置初始字段（批次号、工单号、库存、剩余量、预计库存等）</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqPreValidationHandler extends AbsTqScheduleStepHandler {

    @Resource
    private ITqDataLoadService dataLoadService;

    @Resource
    private TqEngineMapper tqEngineMapper;

    @Resource
    private IncrementService incrementService;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Override
    protected String getStepName() {
        return "S1-前置校验与数据加载";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        // 1. 加载全部基础数据到Context
        dataLoadService.loadAllData(context);

        // 2. 校验施工信息完整性
        validateConstruction(context);

        // 3. 为每条排程记录设置初始字段
        initScheduleFields(context);
    }

    /**
     * 校验成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则中断排程。
     *
     * <p>校验规则：忽略外协规格，只校验非外协规格的施工信息完整性。</p>
     * <p>校验字段：胎胚代码、版本、胎圈代码、钢丝圈代码、三角胶代码、胶料代码、口型板、尺寸</p>
     */
    private void validateConstruction(TqScheduleContext context) {
        String scheduleDate = context.getScheduleDate();
        String batchNo = context.getBatchNo();
        String productionStage = context.getParams().getProductionStage();

        List<EngineConstructionInfo> list = tqEngineMapper.listTqNeedConstruction(scheduleDate, productionStage);
        // 校验忽略掉外协规格，只校验不是外协的规格
//        list = list.stream()
//                .filter(r -> !context.getAssistSpecMap().containsKey(r.getTireRingCode()))
//                .collect(Collectors.toList());

        for (EngineConstructionInfo construction : list) {
            List<String> errorColumns = new ArrayList<>();

            // 源数据（成型排程结果 T_CX_SCHEDULE_RESULT）胎胚代码/版本为空防护：
            // 此时 construction.getEmbryoCode()/getBomDataVersion() 可能为 null，
            // 直接 split(",") 会触发 NPE；且这属于成型排程源数据脏数据，需单独报错让用户感知，
            // 而非走下游"施工表无匹配"的 length<2 校验，避免错误归因。
            String embryoCodeRaw = construction.getEmbryoCode();
            String bomDataVersionRaw = construction.getBomDataVersion();
            if (StringUtils.isBlank(embryoCodeRaw) || StringUtils.isBlank(bomDataVersionRaw)) {
                List<String> sourceErrorColumns = new ArrayList<>();
                if (StringUtils.isBlank(embryoCodeRaw)) {
                    sourceErrorColumns.add("\"" + I18nUtil.getMessage("ui.construction.embryoCode") + "\"");
                }
                if (StringUtils.isBlank(bomDataVersionRaw)) {
                    sourceErrorColumns.add("\"" + I18nUtil.getMessage("ui.construction.embryoVersion") + "\"");
                }
                String tip = StringUtils.format(
                        I18nUtil.getMessage("engine.auto.scheule.construction.validate"),
                        StringUtils.defaultString(embryoCodeRaw, ""),
                        StringUtils.defaultString(bomDataVersionRaw, ""),
                        String.join(",", sourceErrorColumns));
                autoScheduleLogService.insertTqScheduleLog(batchNo, "", "自动排程失败", tip);
                context.interruptSchedule(tip);
                return;
            }

            String embryoCode = embryoCodeRaw.split(",")[0];
            String[] versionArray = bomDataVersionRaw.split(",");
            String embryoVersion = versionArray.length > 0 ? versionArray[0] : "";

            // 胎圈代码为空说明成型排程的胎胚在施工表中找不到匹配记录，直接中断排程
            if (StringUtils.isBlank(construction.getTireRingCode())) {
                String tip = StringUtils.format(
                        I18nUtil.getMessage("engine.auto.scheule.construction.validate"),
                        embryoCode, embryoVersion,
                        "\"" + I18nUtil.getMessage("ui.construction.tireRingCode") + "\"");
                autoScheduleLogService.insertTqScheduleLog(batchNo, "", "自动排程失败", tip);
                context.interruptSchedule(tip);
                return;
            }

            if (construction.getEmbryoCode().split(",").length < 2) {
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.embryoCode") + "\"");
            }
            if (versionArray.length < 2) {
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.embryoVersion") + "\"");
            }
            if (StringUtils.isBlank(construction.getTireRingCode())) {
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.tireRingCode") + "\"");
            }
            if (StringUtils.isBlank(construction.getBeadCode())) {
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.beadCode") + "\"");
            }
            if (StringUtils.isBlank(construction.getApexCode())) {
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.apexCode") + "\"");
            }
            if (StringUtils.isBlank(construction.getHexagonRubberCode())) {
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.hexagonRubberCode") + "\"");
            }
            if (StringUtils.isBlank(construction.getHexagonMouthPlate())) {
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.hexagonMouthPlate") + "\"");
            }
            if (StringUtils.isBlank(construction.getHexagonRubberDimension())) {
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.hexagonRubberDimension") + "\"");
            }

            if (!errorColumns.isEmpty()) {
                String tip = StringUtils.format(
                        I18nUtil.getMessage("engine.auto.scheule.construction.validate"),
                        embryoCode, embryoVersion, String.join(",", errorColumns));
                autoScheduleLogService.insertTqScheduleLog(batchNo, "", "自动排程失败", tip);
                context.interruptSchedule(tip);
                return;
            }
        }
        log.info("[S1] 施工信息校验通过");
    }

    /**
     * 为每条排程记录设置初始字段。
     *
     * <p>设置内容：批次号、工单号、库存、剩余量、昨日中班计划、预计库存、大尺寸阈值、发布状态等。</p>
     */
    private void initScheduleFields(TqScheduleContext context) {
        String batchNo = context.getBatchNo();
        String username = context.getOperator();
        if (StringUtils.isBlank(username)) {
            username = SecurityUtils.getUsername();
        }

        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            // 设置成型批次号（取第一条记录的成型批次号）
            if (StringUtils.isBlank(context.getCxBatchNo()) && StringUtils.isNotEmpty(scheduleVo.getCxBatchNo())) {
                context.setCxBatchNo(scheduleVo.getCxBatchNo());
            }

            // 批次号和工单号
            scheduleVo.setBatchNo(batchNo);
            String orderNo = createOrderNo(batchNo);
            scheduleVo.setOrderNo(orderNo);

            // 库存（6点MES库存，直接使用原始值）
            Double rawStockQty = context.getStockMap().getOrDefault(scheduleVo.getBeadCode(), 0D);
            scheduleVo.setStockQty(rawStockQty);

            // 剩余量（月度剩余）
            scheduleVo.setSurplusQty(Optional.ofNullable(context.getMonthSurplusMap().get(scheduleVo.getBeadCode()))
                    .map(vo -> vo.getMonthRemainQty())
                    .orElse(0D));

            // 当天早班(D日早班)计划量（昨天已排的、属于今天早班的胎圈计划量）
            scheduleVo.setTodayMorningPlanQty(context.getTodayMorningPlanMap().getOrDefault(scheduleVo.getBeadCode(), 0D));

            // 计算14点预计库存 = 6点MES库存 - 早班胎圈预计消耗量 + 早班胎圈计划量
            // 早班胎圈预计消耗量 = 成型1班消耗 × 需求系数
            double coefficient = context.getParams().getDemandCoefficient() == null ? 2D : context.getParams().getDemandCoefficient();
            double morningTqConsume = BigDecimalUtil.mul(
                    scheduleVo.getCxClass1Plan() == null ? 0 : scheduleVo.getCxClass1Plan(), coefficient);
            double planStockQty = BigDecimalUtil.sub(
                    BigDecimalUtil.add(scheduleVo.getStockQty(), scheduleVo.getTodayMorningPlanQty()),
                    morningTqConsume);
            scheduleVo.setPlanStockQty(planStockQty);

            // 大尺寸规格阈值
            scheduleVo.getParams().put(com.zlt.aps.common.engine.constants.EngineConstants.BIG_SIZE_SPEC,
                    context.getParams().getBigSizeSpec());

            // 发布状态
            scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
            scheduleVo.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            scheduleVo.setCreateTime(new Date());
            scheduleVo.setCreateBy(username);
        }

        log.info("[S1] 排程记录初始字段设置完成, 记录数:{}", context.getScheduleList().size());
    }

    /**
     * 创建工单号（批次号+4位定长自增序号）
     */
    private String createOrderNo(String batchNo) {
        return incrementService.getSequence4(batchNo);
    }
}
