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

    /**
     * 查询指定精度类型的最大版本号
     * 版本号格式如：APS_MES_AH01_20260510120430003，字符串MAX比较即可获取最新版本
     *
     * @param precisionType 精度类型（如：硫化精度）
     * @return 最大版本号，无数据时返回null
     */
    String selectMaxDataVersion(@Param("precisionType") String precisionType);

    /**
     * 查询指定精度类型和版本号前缀的最大版本号
     * 用于按版本前缀过滤，如只取APS_MES_AH01前缀的最新版本
     *
     * @param precisionType 精度类型（如：硫化精度）
     * @param versionPrefix 版本号前缀（如：APS_MES_AH01）
     * @return 最大版本号，无数据时返回null
     */
    String selectMaxDataVersionByPrefix(@Param("precisionType") String precisionType,
                                         @Param("versionPrefix") String versionPrefix);
}
