package com.zlt.aps.gsq.engine.handler;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.mapper.GsqEngineMapper;
import com.zlt.aps.gsq.engine.service.IGsqDataLoadService;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * S1: 钢丝圈前置校验与数据加载Handler。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>通过 IGsqDataLoadService 加载全部基础数据到Context</li>
 *   <li>校验施工信息完整性（胎圈代码、钢丝圈代码、BOM用量、钢丝直径）</li>
 *   <li>生成批次号 GSQ+yyyyMMdd+3位序号</li>
 *   <li>为每条排程记录设置初始字段（批次号、工单号、库存、剩余量等）</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqPreValidationHandler extends AbsGsqScheduleStepHandler {

    @Resource
    private IGsqDataLoadService dataLoadService;

    @Resource
    private GsqEngineMapper gsqEngineMapper;

    @Resource
    private IncrementService incrementService;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Override
    protected String getStepName() {
        return "S1-前置校验与数据加载";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        // 1. 加载全部基础数据到Context（胎圈6班次结果、BOM、6点MES库存、机台、工装车、检修、停产、参数）
        dataLoadService.loadAllData(context);

        // 2. 生成批次号
        String batchNo = generateBatchNo(context.getScheduleDate());
        context.setBatchNo(batchNo);

        // 3. 校验施工信息完整性
        validateConstruction(context);

        // 4. 为每条排程记录设置初始字段
        initScheduleFields(context);

        autoScheduleLogService.insertGsqScheduleLog(batchNo, "",
                "S1-前置校验与数据加载完成",
                "排程数据条数：" + context.getScheduleList().size());
    }

    /**
     * 生成批次号，规则：GSQ+yyyyMMdd+3位定长自增序号
     */
    private String generateBatchNo(String scheduleDate) {
        String dateStr = scheduleDate.replace("-", "");
        // 通过Sequence获取3位定长自增序号
        return incrementService.getSequence3(EngineConstants.GSQ_BATCH_NO_PREFIX + dateStr);
    }

    /**
     * 校验施工信息完整性。
     *
     * <p>校验字段：胎圈代码、钢丝圈代码、BOM用量、钢丝直径、英寸</p>
     */
    private void validateConstruction(GsqScheduleContext context) {
        List<EngineConstructionInfo> list = context.getConstructionInfoList();
        List<String> errors = new ArrayList<>();

        for (EngineConstructionInfo construction : list) {
            List<String> errorColumns = new ArrayList<>();

            if (StringUtils.isBlank(construction.getBeadCode())) {
                errorColumns.add("钢丝圈代码");
            }
            if (StringUtils.isBlank(construction.getTireRingCode())) {
                errorColumns.add("胎圈代码");
            }
            if (StringUtils.isBlank(construction.getEmbryoCode())) {
                errorColumns.add("胎胚代码");
            }

            if (!errorColumns.isEmpty()) {
                String tip = "钢丝圈[" + construction.getBeadCode() + "]施工信息缺失字段: " + String.join("、", errorColumns);
                errors.add(tip);
            }
        }

        if (!errors.isEmpty()) {
            String errorMsg = "施工信息完整性校验失败：\n" + String.join("\n", errors);
            context.interruptSchedule(errorMsg);
        }
    }

    /**
     * 为每条排程记录设置初始字段。
     */
    private void initScheduleFields(GsqScheduleContext context) {
        GsqScheduleParams params = context.getParams();
        String batchNo = context.getBatchNo();
        String username = SecurityUtils.getUsername();
        Date now = new Date();
        int[] orderSeq = {0};

        for (GsqScheduleResultVo scheduleVo : context.getScheduleList()) {
            // 批次号
            scheduleVo.setBatchNo(batchNo);
            // 工单号
            scheduleVo.setOrderNo(generateOrderNo(batchNo, orderSeq[0]++));
            // 库存
            scheduleVo.setStockQty(context.getStockMap().getOrDefault(scheduleVo.getSteelRingCode(), 0D));
            // 缠绕盘代码（从钢丝圈-缠绕盘绑定映射回填）
            scheduleVo.setTwiningDiscCode(context.getTwiningDiscCodeMap().get(scheduleVo.getSteelRingCode()));
            // 月计划剩余量（对齐胎圈TQ：从月计划剩余量映射按钢丝圈代码取值）
            double remainQty = Optional.ofNullable(context.getMonthSurplusMap().get(scheduleVo.getSteelRingCode()))
                    .map(vo -> vo.getMonthRemainQty())
                    .orElse(0D);
            scheduleVo.setMonthSurplusQty((int) remainQty);
            // 前日早班计划量
            scheduleVo.setLastMidPlanQty(context.getLastMidPlanMap().getOrDefault(scheduleVo.getSteelRingCode(), 0D));
            // 预计库存 = 当前库存 + 前日早班计划量 - 胎圈1班消耗量
            double planStock = scheduleVo.getStockQty() + scheduleVo.getLastMidPlanQty() - scheduleVo.getTqClass1Plan();
            scheduleVo.setPlanStockQty(planStock);
            // 数据来源：自动排程
            scheduleVo.setDataSource("0");
            // 是否发布：未发布
            scheduleVo.setIsRelease("0");
            // 收尾规格标记默认为非收尾
            scheduleVo.setCloseOutSpecFlag("1");
            // 未排标识默认为已排
            scheduleVo.setUnscheduledFlag("0");
            // 保鲜期超期标记默认为无超期
            scheduleVo.setFreshExpiredFlag("0");
            // 创建人/创建时间
            scheduleVo.setCreateBy(username);
            scheduleVo.setCreateTime(now);
            // 分厂
            scheduleVo.setFactoryCode(context.getFactoryCode());
        }
    }

    /**
     * 生成工单号，规则：批次号+4位定长自增序号
     */
    private String generateOrderNo(String batchNo, int seq) {
        return batchNo + String.format("%04d", seq + 1);
    }
}
