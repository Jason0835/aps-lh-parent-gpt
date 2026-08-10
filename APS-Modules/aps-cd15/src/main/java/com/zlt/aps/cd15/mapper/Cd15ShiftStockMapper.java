package com.zlt.aps.cd15.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/**
 * 斜裁自动滚动班次库存写入Mapper。
 */
@Mapper
public interface Cd15ShiftStockMapper extends CommBaseMapper<Cd15ShiftStock> {

    /** 清理同范围上一版已失效快照。 */
    @Delete("DELETE FROM T_CD15_SHIFT_STOCK WHERE FACTORY_CODE = #{factoryCode} "
            + "AND SHIFT_START_TIME = #{shiftStartTime} AND SHIFT_CODE = #{shiftCode} "
            + "AND IS_DELETE = 1")
    int deleteInvalidByScope(@Param("factoryCode") String factoryCode,
                             @Param("shiftStartTime") Date shiftStartTime,
                             @Param("shiftCode") String shiftCode);

    /** 按目标交班范围失效旧快照。 */
    @Update("UPDATE T_CD15_SHIFT_STOCK SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, "
            + "UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} "
            + "AND SHIFT_START_TIME = #{shiftStartTime} AND SHIFT_CODE = #{shiftCode} "
            + "AND IS_DELETE = 0")
    int logicDeleteByScope(@Param("factoryCode") String factoryCode,
                           @Param("shiftStartTime") Date shiftStartTime,
                           @Param("shiftCode") String shiftCode,
                           @Param("updateBy") String updateBy,
                           @Param("updateTime") Date updateTime);
}
