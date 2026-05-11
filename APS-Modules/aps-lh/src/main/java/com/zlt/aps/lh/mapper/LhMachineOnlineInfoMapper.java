package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/**
 * 硫化在机信息Mapper
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Mapper
public interface LhMachineOnlineInfoMapper extends CommBaseMapper<LhMachineOnlineInfo> {

    /**
     * 根据分厂编号逻辑删除硫化在机信息
     *
     * @param factoryCode 分厂编号
     * @param updateBy    更新者
     * @param updateTime  更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_MACHINE_ONLINE_INFO SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} AND IS_DELETE = 0")
    int logicDeleteByFactoryCode(@Param("factoryCode") String factoryCode, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /**
     * 根据分厂编号和在线日期逻辑删除硫化在机信息
     *
     * @param factoryCode 分厂编号
     * @param onlineDate  在线日期
     * @param updateBy    更新者
     * @param updateTime  更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_MACHINE_ONLINE_INFO SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} AND DATE(ONLINE_DATE) = #{onlineDate} AND IS_DELETE = 0")
    int logicDeleteByFactoryCodeAndOnlineDate(@Param("factoryCode") String factoryCode, @Param("onlineDate") Date onlineDate, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /**
     * 逻辑删除历史重复数据，保留每个历史在线日期DATA_VERSION最大（最新版本）的数据
     * 对于今天以前的每个历史日期+分厂，逻辑删除DATA_VERSION不等于最大DATA_VERSION的所有记录
     *
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_MACHINE_ONLINE_INFO SET IS_DELETE = 1, UPDATE_BY = 'CLEAN_TASK', UPDATE_TIME = NOW() WHERE DATE(ONLINE_DATE) < CURDATE() AND IS_DELETE = 0 AND " +
            "EXISTS (" +
            "SELECT 1 FROM (" +
            "SELECT FACTORY_CODE, DATE(ONLINE_DATE) AS online_day, MAX(DATA_VERSION) AS max_data_version " +
            "FROM T_LH_MACHINE_ONLINE_INFO WHERE DATE(ONLINE_DATE) < CURDATE() AND IS_DELETE = 0 " +
            "GROUP BY FACTORY_CODE, DATE(ONLINE_DATE)" +
            ") latest " +
            "WHERE T_LH_MACHINE_ONLINE_INFO.FACTORY_CODE = latest.FACTORY_CODE " +
            "AND DATE(T_LH_MACHINE_ONLINE_INFO.ONLINE_DATE) = latest.online_day " +
            "AND T_LH_MACHINE_ONLINE_INFO.DATA_VERSION < latest.max_data_version" +
            ")")
    int cleanHistoryDuplicateData();
}
