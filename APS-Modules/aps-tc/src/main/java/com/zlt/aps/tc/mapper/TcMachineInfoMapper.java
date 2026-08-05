package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TcMachineInfoMapper.java
 * 描    述：胎侧机台基础表 Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
@Mapper
public interface TcMachineInfoMapper extends CommBaseMapper<TcMachineInfo> {

    /**
     * 物理删除指定 (FACTORY_CODE, MACHINE_CODE) 的历史逻辑删除墓碑记录。
     * 逻辑删除当前机台前调用，避免 0->1 与旧墓碑冲突唯一索引
     * uk_tc_machine_info_factory_machine (FACTORY_CODE, MACHINE_CODE, IS_DELETE)。
     * 自定义 @Delete 不经 MyBatis-Plus 逻辑删除改写，执行物理删除。
     */
    @Delete("DELETE FROM T_TC_MACHINE_INFO "
            + "WHERE FACTORY_CODE = #{factoryCode} "
            + "AND MACHINE_CODE = #{machineCode} "
            + "AND IS_DELETE = '1'")
    int physicalDeleteTombstones(@Param("factoryCode") String factoryCode,
                                 @Param("machineCode") String machineCode);
}
