package com.zlt.aps.gsq.engine.mapper;

import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqStockShiftConfig;
import com.zlt.aps.gsq.engine.vo.GsqParamsVo;
import com.zlt.aps.gsq.engine.vo.GsqQuotaParam;
import com.zlt.aps.gsq.engine.vo.GsqScheduleBaseInfoVo;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 钢丝圈排程引擎Mapper（6班次制）。
 *
 * <p>负责：</p>
 * <ul>
 *   <li>从胎圈6班次排程结果 + 施工BOM信息汇总钢丝圈基础数据</li>
 *   <li>读写 T_GSQ_SCHEDULE_RESULT 表</li>
 *   <li>同步排程日志表 T_GSQ_SCHEDULE_RESULT_LOG</li>
 *   <li>读取胎圈停产、机台检修等配置</li>
 * </ul>
 *
 * @author APS
 */
public interface GsqEngineMapper {

    /**
     * 根据胎圈6班次排程记录 + 施工BOM，统计出钢丝圈6班次排程记录基础数据。
     *
     * <p>核心SQL逻辑：</p>
     * <ol>
     *   <li>从 T_TQ_SCHEDULE_RESULT 读取胎圈1~6班计划量</li>
     *   <li>关联 T_PRODUCT_CONSTRUCTION_INFO 取 BEAD_CODE(钢丝圈代码)、BOM用量</li>
     *   <li>按钢丝圈代码聚合，6班次计划量 = Σ(胎圈N班计划量 × BOM用量)</li>
     *   <li>过滤掉6班次计划量全为0的记录</li>
     * </ol>
     *
     * @param scheduleDate 排程日期，格式 yyyy-MM-dd
     * @param productionStage 仅投产阶段规格排产标识：0=全部，1=仅投产阶段
     * @return 钢丝圈排程基础数据列表
     */
    List<GsqScheduleResultVo> statGsqScheduleBase(@Param("scheduleDate") String scheduleDate,
                                                  @Param("productionStage") String productionStage);

    /**
     * 获取钢丝圈对应的成型胎胚code和机台code（用于定额计算）。
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return 定额参数列表
     */
    List<GsqQuotaParam> listQuotaParam(@Param("scheduleDate") String scheduleDate,
                                       @Param("productionStage") String productionStage);

    /**
     * 创建自动排程记录（清空当日排程后写入新数据）。
     * @param params 包含 scheduleDate、tqBatchNo、batchNo、username 等
     */
    void createScheduleRecord(Map<String, Object> params);

    /**
     * 删除指定日期的排程数据（物理删除，写入日志后调用）。
     * @param scheduleDate 排程日期
     */
    void deleteGsqSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 删除指定日期的外协排程数据。
     * @param scheduleDate 排程日期
     */
    void deleteGsqAssistSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 把排程数据同步到log表（删除前先备份）。
     * @param scheduleDate 排程日期
     */
    void syncGsqScheduleToLog(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量新增6班次排程结果数据。
     * @param scheduleResultList 排程结果列表
     */
    void batchCreateScheduleResult(@Param("scheduleResultList") List<GsqScheduleResultVo> scheduleResultList);

    /**
     * 批量新增外协排程结果数据。
     * @param scheduleResultList 排程结果列表
     */
    void batchCreateAssistScheduleResult(@Param("scheduleResultList") List<GsqScheduleResultVo> scheduleResultList);

    /**
     * 返回钢丝圈参数计划（T_GSQ_PARAMS 表）。
     * @return 参数列表
     */
    List<GsqParamsVo> listGsqParams();

    /**
     * 查询当前排程的批次号。
     * @param scheduleDate 排程日期 yyyy-MM-dd
     * @return 当前批次号
     */
    String getGsqCurrentBatchNo(@Param("scheduleDate") String scheduleDate);

    /**
     * 根据钢丝圈code查询出关联施工表的其他信息（BOM用量、关联胎圈、钢丝直径等）。
     * @param steelRingCodes 钢丝圈code列表
     * @param productionStage 仅投产阶段规格排产标识
     * @return 排程基础信息列表
     */
    List<GsqScheduleBaseInfoVo> listGsqScheduleBaseInfo(@Param("steelRingCodes") List<String> steelRingCodes,
                                                        @Param("productionStage") String productionStage);

    /**
     * 查询指定日期的排程数据。
     * @param scheduleDate 排程日期
     * @return 排程结果列表
     */
    List<GsqScheduleResultVo> listGsqEnginSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量合并排程结果表（根据唯一字段，做更新或新增）。
     * @param scheduleResultList 排程结果列表
     * @return 影响行数
     */
    int mergeGsqScheduleResult(@Param("scheduleResultList") List<GsqScheduleResultVo> scheduleResultList);

    /**
     * 查询出钢丝圈需要的施工信息字段（S1阶段校验完整性用）。
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return 施工信息列表
     */
    List<EngineConstructionInfo> listGsqNeedConstruction(@Param("scheduleDate") String scheduleDate,
                                                         @Param("productionStage") String productionStage);

    /**
     * 获得外协规格列表。
     * @return 外协规格code列表
     */
    List<String> listAssistSpec();

    /**
     * 批量更新批次号和工单号。
     * @param scheduleResultVoList 排程结果列表
     */
    void batchUpdateBatchNoAndOrderNo(@Param("list") List<GsqScheduleResultVo> scheduleResultVoList);

    /**
     * 读取胎圈6班次排程结果（按胎圈代码聚合），用于S2阶段BOM分解计算。
     *
     * @param scheduleDate 排程日期 yyyy-MM-dd
     * @return 胎圈6班次计划量列表，每条记录包含 beadCode + class1PlanQty~class6PlanQty
     */
    List<Map<String, Object>> listTqScheduleResult6Class(@Param("scheduleDate") String scheduleDate);

    /**
     * 读取胎圈6班次停产班次配置（从T_TQ_STOCK_SHIFT_CONFIG或工作日历）。
     *
     * @param scheduleDate 排程日期
     * @return 停产班次配置列表
     */
    List<Map<String, Object>> listTqStopShiftConfig(@Param("scheduleDate") String scheduleDate);

    /**
     * 读取钢丝圈6班次停产班次配置。
     *
     * @param scheduleDate 排程日期
     * @return 停产班次配置列表
     */
    List<Map<String, Object>> listGsqStopShiftConfig(@Param("scheduleDate") String scheduleDate);

    /**
     * 读取钢丝圈备库班数配置（T_GSQ_STOCK_SHIFT_CONFIG，按 MIN_MACHINE_QTY 升序）。
     *
     * <p>对齐胎圈 {TqStockShiftConfig} 的 DepthConfig 区间方式，按分厂过滤。</p>
     *
     * @param factoryCode 分厂编码
     * @return 备库班数配置列表
     */
    List<GsqStockShiftConfig> listGsqStockShiftConfig(@Param("factoryCode") String factoryCode);

    /**
     * 读取机台检修计划（对齐胎圈TQ：DOWNTIME_DATE 从排程日期起 +3 天范围）。
     *
     * @param scheduleDate 排程日期
     * @return 检修计划列表，包含 machineCode、downtimeDate、downtimeShift
     */
    List<Map<String, Object>> listMachineMaintenancePlan(@Param("scheduleDate") String scheduleDate);
}
