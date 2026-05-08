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
}
