package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcLossSetting;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TcLossSettingMapper extends CommBaseMapper<TcLossSetting> {

    /**
     * 物理清理同 (FACTORY_CODE, SIDEWALL_CODE, MACHINE_CODE) 的历史墓碑(IS_DELETE=1)，
     * 避免逻辑删除 0->1 时唯一索引 uk_tc_loss_setting_factory_sidewall_machine 冲突（#23294）。
     * 自定义 @Delete 不经 MyBatis-Plus 逻辑删除改写，执行物理删除。
     */
    @Delete("DELETE FROM T_TC_LOSS_SETTING "
            + "WHERE FACTORY_CODE = #{factoryCode} "
            + "AND SIDEWALL_CODE = #{sidewallCode} "
            + "AND MACHINE_CODE = #{machineCode} "
            + "AND IS_DELETE = '1'")
    int physicalDeleteTombstones(@Param("factoryCode") String factoryCode,
                                 @Param("sidewallCode") String sidewallCode,
                                 @Param("machineCode") String machineCode);
}
