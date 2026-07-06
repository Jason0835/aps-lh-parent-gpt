package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.entity.TmScheFinishQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/**
 * 胎面排程完成量回报 Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface TmScheFinishQtyMapper extends CommBaseMapper<TmScheFinishQty> {

    /**
     * 按分厂+排程日期逻辑删除胎面排程完成量
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期
     * @param updateBy     更新者
     * @param updateTime   更新时间
     * @return 影响行数
     */
    @Update("UPDATE T_TM_SCHE_FINISH_QTY SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} "
            + "WHERE FACTORY_CODE = #{factoryCode} AND DATE(SCHEDULE_DATE) = #{scheduleDate} AND IS_DELETE = 0")
    int logicDeleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode,
                                                @Param("scheduleDate") Date scheduleDate,
                                                @Param("updateBy") String updateBy,
                                                @Param("updateTime") Date updateTime);
}
