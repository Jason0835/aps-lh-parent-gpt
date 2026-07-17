package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.domain.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 胎侧自动排程数据加载 Mapper。
 *
 * <p>承接自动排程数据加载阶段需要的跨业务表查询，避免在 Service 中维护原生 SQL。</p>
 */
@Mapper
public interface TcAutoScheduleDataLoadMapper {

    /**
     * 查询成型排程和施工信息关联数据。
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程日期
     * @return 成型需求和施工信息行数据
     */
    List<TcFormingDemandRowVo> selectFormingDemandRows(@Param("factoryCode") String factoryCode,
                                                       @Param("scheduleDate") Date scheduleDate);

    /**
     * 查询成型排程结果（仅成型表，含各班次示方书编号），用于 RECIPE 模式逐班解析施工。
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程日期
     * @return 成型排程行数据（含 CLASS1~8_RECIPE_NO）
     */
    List<TcFormingDemandRecipeRowVo> selectFormingDemandRowsByRecipe(@Param("factoryCode") String factoryCode,
                                                                     @Param("scheduleDate") Date scheduleDate);

    /**
     * 按 (CONSTRUCTION_CODE, CONSTRUCTION_VERSION) 批量查询施工胎侧属性，用于 RECIPE 模式逐班关联。
     *
     * @param factoryCode          工厂编号
     * @param embryoCodes          胚胎代码集合（对应 CONSTRUCTION_CODE）
     * @param constructionVersions 施工版本集合（对应 CLASSn_RECIPE_NO）
     * @return 施工胎侧属性行数据
     */
    List<TcConstructionSidewallRowVo> selectConstructionInfoRows(@Param("factoryCode") String factoryCode,
                                                              @Param("embryoCodes") Collection<String> embryoCodes,
                                                              @Param("constructionVersions") Collection<String> constructionVersions);

    /**
     * 查询人工插单可选的有效胎侧施工版本。
     *
     * @param factoryCode 工厂编码
     * @return 胎侧施工选项基础行
     */
    List<TcConstructionSidewallRowVo> selectManualConstructionOptions(@Param("factoryCode") String factoryCode);

    /**
     * 查询指定工序的工作日历。
     *
     * @param factoryCode    工厂编号
     * @param procCode       工序编码
     * @param productionDate 生产日期
     * @return 工作日历行数据
     */
    List<TcWorkCalendarRowVo> selectWorkCalendarRows(@Param("factoryCode") String factoryCode,
                                                     @Param("procCode") String procCode,
                                                     @Param("productionDate") Date productionDate);

    /**
     * 按日期区间查询指定工序工作日历。
     *
     * @param factoryCode 工厂编号
     * @param procCode    工序编码
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 日期区间内工作日历
     */
    List<TcWorkCalendarRowVo> selectWorkCalendarRowsByRange(@Param("factoryCode") String factoryCode,
                                                            @Param("procCode") String procCode,
                                                            @Param("startDate") Date startDate,
                                                            @Param("endDate") Date endDate);
    /**
     * 查询月计划定稿实验规格数据。
     *
     * @param factoryCode        工厂编号
     * @param yearMonth          年月，格式 yyyyMM
     * @param dayColumn          月计划日期列，格式 DAY_1 到 DAY_31
     * @param experimentPlanDate 月计划定稿生产日期
     * @param constructionStage  实验规格施工阶段编码
     * @return 实验规格月计划行数据
     */
    List<TcExperimentSpecMonthPlanRowVo> selectExperimentSpecMonthPlanRows(@Param("factoryCode") String factoryCode,
                                                                           @Param("yearMonth") Integer yearMonth,
                                                                           @Param("dayColumn") String dayColumn,
                                                                           @Param("experimentPlanDate") Date experimentPlanDate,
                                                                           @Param("constructionStage") String constructionStage);
}
