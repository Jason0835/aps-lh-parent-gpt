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
     * 逻辑删除今天及今天之前所有数据（将IS_DELETE置为1）
     * 用于清理任务：先删除所有历史数据（含今天），再从MES重新抓取每天最新版本数据
     *
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_MACHINE_ONLINE_INFO SET IS_DELETE = 1, UPDATE_BY = 'CLEAN_TASK', UPDATE_TIME = NOW() WHERE DATE(ONLINE_DATE) <= CURDATE() AND IS_DELETE = 0")
    int logicDeleteAllBeforeToday();
}
