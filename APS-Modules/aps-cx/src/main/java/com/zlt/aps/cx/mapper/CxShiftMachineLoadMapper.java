package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.schedule.CxShiftMachineLoad;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 班次级机台胎胚负荷映射Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface CxShiftMachineLoadMapper extends BaseMapper<CxShiftMachineLoad> {

    /**
     * 查询指定日期最后一个班次的负荷记录
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  工厂编码
     * @return 该日期最大班次序号的负荷记录列表
     */
    @Select("SELECT * FROM T_CX_SHIFT_MACHINE_LOAD " +
            "WHERE SCHEDULE_DATE = #{scheduleDate} " +
            "AND FACTORY_CODE = #{factoryCode} " +
            "AND SHIFT_ORDER = (SELECT MAX(SHIFT_ORDER) FROM T_CX_SHIFT_MACHINE_LOAD " +
            "                  WHERE SCHEDULE_DATE = #{scheduleDate} AND FACTORY_CODE = #{factoryCode})")
    List<CxShiftMachineLoad> selectLastShiftByDate(@Param("scheduleDate") LocalDate scheduleDate,
                                                    @Param("factoryCode") String factoryCode);

    /**
     * 删除指定日期的负荷记录
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  工厂编码
     * @return 删除条数
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM T_CX_SHIFT_MACHINE_LOAD " +
            "WHERE SCHEDULE_DATE = #{scheduleDate} AND FACTORY_CODE = #{factoryCode}")
    int deleteByDate(@Param("scheduleDate") LocalDate scheduleDate,
                     @Param("factoryCode") String factoryCode);
}
