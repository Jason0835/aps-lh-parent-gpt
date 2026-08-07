package com.zlt.aps.cd90.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheFinishQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/**
 * 直裁排程每日完成量Mapper。
 */
@Mapper
public interface Cd90ScheFinishQtyMapper extends CommBaseMapper<Cd90ScheFinishQty> {

    /**
     * 按工厂和归属日期逻辑删除旧回报。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES完成量归属日期
     * @param updateBy 更新人
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE T_CD90_SCHE_FINISH_QTY SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, "
            + "UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} "
            + "AND DATE(SCHEDULE_DATE) = DATE(#{scheduleDate}) AND IS_DELETE = 0")
    int logicDeleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode,
                                                @Param("scheduleDate") Date scheduleDate,
                                                @Param("updateBy") String updateBy,
                                                @Param("updateTime") Date updateTime);
}
