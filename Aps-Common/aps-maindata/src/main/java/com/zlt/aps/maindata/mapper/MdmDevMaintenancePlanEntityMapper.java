package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * APS设备保养计划Mapper接口
 */
@Mapper
public interface MdmDevMaintenancePlanEntityMapper extends BaseMapper<MdmDevMaintenancePlan> {

    /**
     * 根据唯一键查询
     * @param devCode 设备机台
     * @param precisionType 精度类型
     * @param factoryCode 厂别
     * @return 查询结果
     */
    MdmDevMaintenancePlan selectByUniqueKey(@Param("devCode") String devCode, 
                                             @Param("precisionType") String precisionType, 
                                             @Param("factoryCode") String factoryCode);

    /**
     * 批量根据唯一键查询
     * @param list 数据列表
     * @return 查询结果
     */
    List<MdmDevMaintenancePlan> selectByUniqueKeyList(@Param("list") List<MdmDevMaintenancePlan> list);
}
