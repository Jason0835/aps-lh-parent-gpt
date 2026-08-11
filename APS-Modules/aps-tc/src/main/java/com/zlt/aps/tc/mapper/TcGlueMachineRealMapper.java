package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcGlueMachineReal;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TcGlueMachineRealMapper extends CommBaseMapper<TcGlueMachineReal> {

    /**
     * 物理清理同 (FACTORY_CODE, GLUE_CODE, MACHINE_CODE) 的历史墓碑(IS_DELETE=1)，
     * 避免逻辑删除 0->1 时唯一索引 uk_tc_glue_machine_real_factory_glue_machine 冲突（#23310）。
     * 自定义 @Delete 不经 MyBatis-Plus 逻辑删除改写，执行物理删除。
     */
    @Delete("DELETE FROM T_TC_GLUE_MACHINE_REAL "
            + "WHERE FACTORY_CODE = #{factoryCode} "
            + "AND GLUE_CODE = #{glueCode} "
            + "AND MACHINE_CODE = #{machineCode} "
            + "AND IS_DELETE = '1'")
    int physicalDeleteTombstones(@Param("factoryCode") String factoryCode,
                                 @Param("glueCode") String glueCode,
                                 @Param("machineCode") String machineCode);
}
