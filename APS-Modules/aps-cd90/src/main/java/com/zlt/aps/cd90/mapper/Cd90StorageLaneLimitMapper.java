package com.zlt.aps.cd90.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface Cd90StorageLaneLimitMapper extends CommBaseMapper<Cd90StorageLaneLimit> {

    /**
     * 逻辑删除指定工厂、日期和班次的库排状态。
     *
     * @param factoryCode 工厂编码
     * @param laneDate 库排日期
     * @param shiftCode 班次编码
     * @param updateBy 更新人
     * @param updateTime 更新时间
     * @return 更新数量
     */
    int logicDeleteByScope(@Param("factoryCode") String factoryCode,
                           @Param("laneDate") Date laneDate,
                           @Param("shiftCode") String shiftCode,
                           @Param("updateBy") String updateBy,
                           @Param("updateTime") Date updateTime);
}
