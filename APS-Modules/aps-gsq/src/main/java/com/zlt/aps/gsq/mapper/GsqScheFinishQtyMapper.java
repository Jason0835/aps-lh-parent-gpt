package com.zlt.aps.gsq.mapper;

import com.zlt.aps.gsq.api.domain.entity.GsqScheFinishQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/**
 * 钢丝圈排程完成量回报Mapper
 *
 * @author APS Team
 * @since 2026/08/14
 */
@Mapper
public interface GsqScheFinishQtyMapper extends CommBaseMapper<GsqScheFinishQty> {

    /**
     * 根据分厂编号和排程日期逻辑删除钢丝圈排程完成量数据
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期
     * @param updateBy     更新者
     * @param updateTime   更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_GSQ_SCHE_FINISH_QTY SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} AND DATE(SCHEDULE_DATE) = #{scheduleDate} AND IS_DELETE = 0")
    int logicDeleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode, @Param("scheduleDate") Date scheduleDate, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);
}
