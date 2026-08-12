package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 逻辑删除指定分厂所有未删除的设备保养计划数据
     * 用于全量同步前清理旧数据（不限精度类型，按分厂清理）
     * WHERE必须包含FACTORY_CODE业务主键，否则会被BlockAttackInnerInterceptor拦截
     *
     * @param factoryCode 分厂编号
     * @return 受影响行数
     */
    @Update("UPDATE T_MDM_DEV_MAINTENANCE_PLAN SET IS_DELETE = 1, UPDATE_BY = 'MES', UPDATE_TIME = NOW() WHERE FACTORY_CODE = #{factoryCode} AND IS_DELETE = 0")
    int logicDeleteByFactoryCode(@Param("factoryCode") String factoryCode);

    /**
     * 逻辑删除指定分厂和精度类型的设备保养计划数据
     * 用于按精度类型同步前清理旧数据（精确匹配，如：硫化精度）
     * WHERE必须包含FACTORY_CODE业务主键，否则会被BlockAttackInnerInterceptor拦截
     *
     * @param factoryCode 分厂编号
     * @param precisionType 精度类型（精确匹配）
     * @return 受影响行数
     */
    @Update("UPDATE T_MDM_DEV_MAINTENANCE_PLAN SET IS_DELETE = 1, UPDATE_BY = 'MES', UPDATE_TIME = NOW() WHERE FACTORY_CODE = #{factoryCode} AND PRECISION_TYPE = #{precisionType} AND IS_DELETE = 0")
    int logicDeleteByFactoryCodeAndPrecisionType(@Param("factoryCode") String factoryCode, @Param("precisionType") String precisionType);

    /**
     * 逻辑删除指定分厂和精度类型前缀的设备保养计划数据
     * 用于成型精度同步前清理旧数据（成型精度15天/成型精度60天都匹配）
     * WHERE必须包含FACTORY_CODE业务主键，否则会被BlockAttackInnerInterceptor拦截
     *
     * @param factoryCode 分厂编号
     * @param precisionTypePrefix 精度类型前缀（如：成型精度）
     * @return 受影响行数
     */
    @Update("UPDATE T_MDM_DEV_MAINTENANCE_PLAN SET IS_DELETE = 1, UPDATE_BY = 'MES', UPDATE_TIME = NOW() WHERE FACTORY_CODE = #{factoryCode} AND PRECISION_TYPE LIKE CONCAT(#{precisionTypePrefix}, '%') AND IS_DELETE = 0")
    int logicDeleteByFactoryCodeAndPrecisionTypePrefix(@Param("factoryCode") String factoryCode, @Param("precisionTypePrefix") String precisionTypePrefix);
}
