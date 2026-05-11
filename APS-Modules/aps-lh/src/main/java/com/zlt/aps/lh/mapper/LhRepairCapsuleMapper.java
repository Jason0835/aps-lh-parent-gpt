package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/**
 * 胶囊已使用次数Mapper
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Mapper
public interface LhRepairCapsuleMapper extends CommBaseMapper<LhRepairCapsule> {

    /**
     * 根据分厂编号逻辑删除胶囊已使用次数
     *
     * @param factoryCode 分厂编号
     * @param updateBy    更新者
     * @param updateTime  更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_REPAIR_CAPSULE SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} AND IS_DELETE = 0")
    int logicDeleteByFactoryCode(@Param("factoryCode") String factoryCode, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /**
     * 根据分厂编号和获取日期逻辑删除胶囊已使用次数
     *
     * @param factoryCode 分厂编号
     * @param obtainTime  获取日期
     * @param updateBy    更新者
     * @param updateTime  更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_REPAIR_CAPSULE SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} AND DATE(OBTAIN_TIME) = #{obtainTime} AND IS_DELETE = 0")
    int logicDeleteByFactoryCodeAndObtainTime(@Param("factoryCode") String factoryCode, @Param("obtainTime") Date obtainTime, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /**
     * 逻辑删除历史重复数据，保留每个历史获取日期DATA_VERSION最大（最新版本）的数据
     *
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_REPAIR_CAPSULE SET IS_DELETE = 1, UPDATE_BY = 'CLEAN_TASK', UPDATE_TIME = NOW() WHERE DATE(OBTAIN_TIME) < CURDATE() AND IS_DELETE = 0 AND " +
            "EXISTS (" +
            "SELECT 1 FROM (" +
            "SELECT FACTORY_CODE, DATE(OBTAIN_TIME) AS obtain_day, MAX(DATA_VERSION) AS max_data_version " +
            "FROM T_LH_REPAIR_CAPSULE WHERE DATE(OBTAIN_TIME) < CURDATE() AND IS_DELETE = 0 " +
            "GROUP BY FACTORY_CODE, DATE(OBTAIN_TIME)" +
            ") latest " +
            "WHERE T_LH_REPAIR_CAPSULE.FACTORY_CODE = latest.FACTORY_CODE " +
            "AND DATE(T_LH_REPAIR_CAPSULE.OBTAIN_TIME) = latest.obtain_day " +
            "AND T_LH_REPAIR_CAPSULE.DATA_VERSION < latest.max_data_version" +
            ")")
    int cleanHistoryDuplicateData();
}
