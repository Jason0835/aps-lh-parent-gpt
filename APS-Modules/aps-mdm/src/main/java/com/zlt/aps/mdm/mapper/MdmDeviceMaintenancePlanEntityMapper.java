package com.zlt.aps.mdm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.dto.MouldMonthUseDto;
import com.zlt.aps.mdm.api.domain.entity.MdmDeviceMaintenancePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 基础数据-设备维护计划
 */
@Mapper
public interface MdmDeviceMaintenancePlanEntityMapper extends BaseMapper<MdmDeviceMaintenancePlan> {
    /**
     * 根据分厂，年月，查询特定模具的可用信息
     *
     * @param factoryCode   分厂
     * @param year          年
     * @param month         月
     * @param mouldCodeList 模具编码集合
     * @return
     */
    List<MouldMonthUseDto> getMonthMaintenanceMould(@Param("factoryCode") String factoryCode,
                                                    @Param("year") int year,
                                                    @Param("month") int month,
                                                    @Param("mouldCodeList") List<String> mouldCodeList);
}