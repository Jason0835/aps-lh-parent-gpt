package com.zlt.aps.gsq.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.common.engine.mapper.CommonMapper;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.mapper.GsqEngineMapper;
import com.zlt.aps.gsq.engine.mapper.GsqEngineStockMapper;
import com.zlt.aps.gsq.engine.service.*;
import com.zlt.aps.gsq.engine.template.AbsGsqScheduleTemplate;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.*;

@Slf4j
@Service
public class GsqEngineServiceImpl implements GsqEngineService {

    @Resource
    private GsqEngineMapper gsqEngineMapper;
    @Resource
    private GsqEngineStockService gsqEngineStockService;
    @Resource
    private GsqEngineMachineService gsqEngineMachineService;
    @Resource
    private IncrementService incrementService;
    @Resource
    private GsqEngineLossService gsqEngineLossService;
    @Resource
    private GsqEngineMonthSurplusService gsqEngineMonthSurplusService;
    @Resource
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;
    @Resource
    private CommonMapper commonMapper;
    @Resource
    private GsqEngineStockMapper gsqEngineStockMapper;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;
    /**
     * 新6班制排程模板（基于模板方法+策略链架构）
     */
    @Resource
    private AbsGsqScheduleTemplate gsqScheduleTemplate;
    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符
    private final static BigDecimal HOUR24 = new BigDecimal("24"); // 24小时
    private final static String DEFAULT_TOOL_CAPACITY = "110"; // 工装容量默认值
    private final static String DEFAULT_PRODUCT_STOCK_HOUR = "24"; // 保库存供应时长
    private final static String DEFAULT_LARGE_DEMAND = "2000"; // 需求量超过该值的算大需求量规格，库存应该需要控制，且超过该值早夜班对半分
    private final static String DEFAULT_BIG_SIZE_SPEC = "17"; // 大尺寸阈值，超过该尺寸的规格比较难做，不要集中在一个班做
    private final static String DEFAULT_EQUAL_SHARE_THRESHOLD = "500"; // 需求量超过该值早夜班对半分

    /**
     * 钢丝圈胶自动排程（6班制新架构入口）。
     *
     * <p>基于模板方法 + 策略链架构，按 S1→S2→S3→S3.5→S4→S5→S5.5→S6 八阶段执行。</p>
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param factoryCode  分厂编码
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoGsqSchedule(String scheduleDate, String factoryCode) {
        log.info("========== 钢丝圈6班制自动排程启动, scheduleDate={}, factoryCode={} ==========", scheduleDate, factoryCode);

        // 1. 构建排程上下文
        GsqScheduleContext context = new GsqScheduleContext();
        context.setScheduleDate(scheduleDate);
        context.setFactoryCode(factoryCode);
        context.setOperator(SecurityUtils.getUsername());

        // 2. 执行模板方法（S1~S6八阶段）
        gsqScheduleTemplate.execute(context);

        // 3. 检查中断情况
        if (context.isInterrupted()) {
            log.error("钢丝圈排程中断: {}", context.getInterruptReason());
            autoScheduleLogService.insertGsqScheduleLog(context.getBatchNo(), "",
                    "自动排程失败", "中断原因：" + context.getInterruptReason());
            throw new RuntimeException("钢丝圈排程失败: " + context.getInterruptReason());
        }

        log.info("========== 钢丝圈6班制自动排程完成, 插入记录数: {} ==========", context.getInsertedCount());
    }

    /**
     * 创建批次号
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @return 批次号
     */
    private String createBatchNo(String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence3(EngineConstants.GSQ_BATCH_NO_PREFIX + scheduleDate);
    }

    /**
     * 创建工单号
     * @param batchNo 批次号
     * @return 工单号
     */
    private String createOrderNo(String batchNo) {
        return incrementService.getSequence4(batchNo);
    }

    /**
     * 根据钢丝圈代码列表查询施工基础信息（用于插单前规格校验和施工字段回填）。
     *
     * <p>委派给 GsqEngineMapper.listGsqScheduleBaseInfo，productionStage 固定为 "1"
     * （仅投产阶段规格），与自动排程口径保持一致。</p>
     *
     * @param steelRingCodes 钢丝圈代码列表
     * @return 施工基础信息列表，空列表表示施工不存在
     */
    @Override
    public List<GsqScheduleBaseInfoVo> listGsqScheduleBaseInfo(List<String> steelRingCodes) {
        if (org.apache.commons.collections.CollectionUtils.isEmpty(steelRingCodes)) {
            return java.util.Collections.emptyList();
        }
        // productionStage=1 表示仅投产阶段规格（与自动排程口径一致）
        return gsqEngineMapper.listGsqScheduleBaseInfo(steelRingCodes, "1");
    }

    /**
     * 生成钢丝圈插单的批次号和工单号。
     *
     * <p>规则与自动排程保持一致：批次号 = 前缀 + yyyyMMdd + 序号；工单号 = 批次号 + 序号。</p>
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @return 长度为2的数组：[0]=批次号，[1]=工单号
     */
    @Override
    public String[] generateBatchNoAndOrderNo(String scheduleDate) {
        String batchNo = createBatchNo(scheduleDate);
        String orderNo = createOrderNo(batchNo);
        return new String[]{batchNo, orderNo};
    }

    /**
     * 获取工序参数map
     * @return
     */
    private GsqScheduleParams loadParams() {
        List<GsqParamsVo> list = this.gsqEngineMapper.listGsqParams();
        Map<String, String> paramsMap = list.stream()
                .collect(Collectors.toMap(GsqParamsVo::getParamCode, GsqParamsVo::getParamValue));

        GsqScheduleParams params = new GsqScheduleParams();

        params.setProductionStage(paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE));
        params.setLossRate(getDouble(paramsMap.get(EngineConstants.LOSS_RATE)));
        params.setMergeThreshold(getDouble(paramsMap.get(EngineConstants.MERGE_PLAN_THRESHOLD)));
        params.setCloseOutNum(getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));
        params.setToolCapacity(getDouble(paramsMap.getOrDefault(EngineConstants.TOOL_CAPACITY, DEFAULT_TOOL_CAPACITY)));
        BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        params.setProductStockDay(productStockHour.divide(HOUR24, 2, RoundingMode.HALF_UP).doubleValue()); // 小时换算成天数
        params.setLargeDemand(getDouble(paramsMap.getOrDefault(EngineConstants.LARGE_DEMAND, DEFAULT_LARGE_DEMAND)));
        params.setBigSizeSpec(BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.BIG_SIZE_SPEC, DEFAULT_BIG_SIZE_SPEC)));
        params.setStockLossRate(getDouble(paramsMap.getOrDefault(EngineConstants.STOCK_LOSS_RATE, "0")));
        params.setStockRatio(BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.STOCK_RATIO, "1")));
        params.setMidSpec(paramsMap.getOrDefault(EngineConstants.MID_SPEC, "").split(","));
        params.setNightSpec(paramsMap.getOrDefault(EngineConstants.NIGHT_SPEC, "").split(","));
        params.setSupplyTime(getDoubleOrDefault(paramsMap.get(EngineConstants.SUPPLY_TIME_PASS), 12D));
        params.setEqualShareThreshold(new BigDecimal(paramsMap.getOrDefault(EngineConstants.EQUAL_SHARE_THRESHOLD, DEFAULT_EQUAL_SHARE_THRESHOLD))); // 平分阈值

        return params;
    }

    /**
     * 获取钢丝圈对应的成型胎胚code和机台code
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @return
     */
    private Map<String, String> getQuotaParamMap(String scheduleDate, String productionStage) {
        List<GsqQuotaParam> list = gsqEngineMapper.listQuotaParam(scheduleDate, productionStage);
        Map<String, String> map = list.stream().collect(Collectors.toMap(GsqQuotaParam::getSteelRingCode, GsqQuotaParam::getQuotaKeys));
        return map == null ? new HashMap<>() : map;
    }

    /**
     * 自动排程基础表的数据日志
     * @param batchNo 自动排程批次号
     * @param twiningDiscMachineMap 缠绕盘和机台关系集合（key = 规格尺寸~排列方式）
     * @param twiningDiscMap 获得钢丝圈代码和缠绕盘集合（value = 规格尺寸~排列方式）map
     * @param specifyCanMachineMap 定点机台和机台的限制作业集合
     * @param specifyNotMachineMap 定点集合和机台的不可作业集合
     * @param planStockMap 16点预计库存集合
     * @param lossRateMap 耗损率集合
     * @param monthSurplus 月度计划剩余量、完成量集合
     * @param params 参数设置集合
     */
    private void baseDataLog(String batchNo, Map<String, String> twiningDiscMachineMap, Map<String, String> twiningDiscMap, Map<String, String> specifyCanMachineMap,
                             Map<String, String> specifyNotMachineMap, Map<String, Double> planStockMap, Map<String, Double> lossRateMap,
                             Map<String, GsqMonthSurplusVo> monthSurplus, GsqScheduleParams params) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("缠绕盘和机台关系集合（key=规格尺寸~排列方式）：" + toJSONString(twiningDiscMachineMap)).append(division);
        logDetail.append("钢丝圈代码和缠绕盘计划（value=规格尺寸~排列方式）：" + toJSONString(twiningDiscMap)).append(division);
        logDetail.append("定点机台和机台的限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点集合和机台的不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("16点预计库存集合：" + toJSONString(planStockMap)).append(division);
        logDetail.append("耗损率集合：" + toJSONString(lossRateMap)).append(division);
        logDetail.append("月度计划剩余量、完成量集合：" + toJSONString(monthSurplus)).append(division);
        logDetail.append("参数设置集合：" + toJSONString(params)).append(division);
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "自动排程基础表的数据日志", logDetail.toString());
    }
}
