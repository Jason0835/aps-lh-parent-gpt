package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.domain.vo.TmFormingDemandRowVo;
import com.zlt.aps.tm.domain.vo.TmWorkCalendarRowVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 胎面自动排程数据加载 Mapper。
 *
 * <p>承接自动排程数据加载阶段需要的跨业务表查询，避免在 Service 中维护原生 SQL。</p>
 */
@Mapper
public interface TmAutoScheduleDataLoadMapper {

    /**
     * 查询成型排程和施工信息关联数据。
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程日期
     * @return 成型需求和施工信息行数据
     */
    List<TmFormingDemandRowVo> selectFormingDemandRows(@Param("factoryCode") String factoryCode,
                                                       @Param("scheduleDate") Date scheduleDate);

    /**
     * 查询指定工序的工作日历。
     *
     * @param factoryCode    工厂编号
     * @param procCode       工序编码
     * @param productionDate 生产日期
     * @return 工作日历行数据
     */
    List<TmWorkCalendarRowVo> selectWorkCalendarRows(@Param("factoryCode") String factoryCode,
                                                     @Param("procCode") String procCode,
                                                     @Param("productionDate") Date productionDate);
}
