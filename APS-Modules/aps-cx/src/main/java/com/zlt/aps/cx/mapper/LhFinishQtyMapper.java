package com.zlt.aps.cx.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 硫化日完成量与班次完成量查询Mapper（跨模块查询LH表）
 *
 * @author APS Team
 */
@Mapper
public interface LhFinishQtyMapper {

    /**
     * 查询T_LH_DAY_FINISH_QTY表，按日期范围（本月1日~排程日当天）并按工厂+物料聚合日完成量。
     *
     * @param factoryCodes  厂别列表
     * @param materialCodes 物料编码列表
     * @param monthStart    本月1日开始时间
     * @param nextDayStart  排程日次日开始时间（不含）
     * @return 聚合结果：[{FACTORY_CODE, MATERIAL_CODE, TOTAL_FINISH_QTY}, ...]
     */
    @Select("<script>" +
            "SELECT FACTORY_CODE, MATERIAL_CODE, SUM(DAY_FINISH_QTY) AS TOTAL_FINISH_QTY " +
            "FROM T_LH_DAY_FINISH_QTY " +
            "WHERE FINISH_DATE &gt;= #{monthStart} AND FINISH_DATE &lt; #{nextDayStart} " +
            "AND FACTORY_CODE IN " +
            "<foreach item='code' collection='factoryCodes' open='(' separator=',' close=')'>#{code}</foreach>" +
            "AND MATERIAL_CODE IN " +
            "<foreach item='code' collection='materialCodes' open='(' separator=',' close=')'>#{code}</foreach>" +
            "AND (IS_DELETE = 0 OR IS_DELETE IS NULL) " +
            "GROUP BY FACTORY_CODE, MATERIAL_CODE" +
            "</script>")
    List<Map<String, Object>> sumDayFinishQty(@Param("factoryCodes") List<String> factoryCodes,
                                              @Param("materialCodes") List<String> materialCodes,
                                              @Param("monthStart") Date monthStart,
                                              @Param("nextDayStart") Date nextDayStart);

    /**
     * 查询T_LH_SCHE_FINISH_QTY表，按排程日当天并按工厂+物料聚合一班（夜班）完成量。
     *
     * @param factoryCodes  厂别列表
     * @param materialCodes 物料编码列表
     * @param scheduleDate  排程日期当天开始时间
     * @param nextDayStart  排程日次日开始时间（不含）
     * @return 聚合结果：[{FACTORY_CODE, MATERIAL_CODE, TOTAL_FINISH_QTY}, ...]
     */
    @Select("<script>" +
            "SELECT FACTORY_CODE, MATERIAL_CODE, SUM(CLASS1_FINISH_QTY) AS TOTAL_FINISH_QTY " +
            "FROM T_LH_SCHE_FINISH_QTY " +
            "WHERE SCHEDULE_DATE &gt;= #{scheduleDate} AND SCHEDULE_DATE &lt; #{nextDayStart} " +
            "AND FACTORY_CODE IN " +
            "<foreach item='code' collection='factoryCodes' open='(' separator=',' close=')'>#{code}</foreach>" +
            "AND MATERIAL_CODE IN " +
            "<foreach item='code' collection='materialCodes' open='(' separator=',' close=')'>#{code}</foreach>" +
            "AND (IS_DELETE = 0 OR IS_DELETE IS NULL) " +
            "GROUP BY FACTORY_CODE, MATERIAL_CODE" +
            "</script>")
    List<Map<String, Object>> sumScheFinishQty(@Param("factoryCodes") List<String> factoryCodes,
                                               @Param("materialCodes") List<String> materialCodes,
                                               @Param("scheduleDate") Date scheduleDate,
                                               @Param("nextDayStart") Date nextDayStart);
}
