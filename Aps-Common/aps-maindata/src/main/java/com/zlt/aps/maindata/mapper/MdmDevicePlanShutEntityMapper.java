package com.zlt.aps.maindata.mapper;

import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmDevicePlanShutMapper.java
 * 描    述：0106基础数据_设备计划停机Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-04
 */
@Mapper
public interface MdmDevicePlanShutEntityMapper extends CommBaseMapper<MdmDevicePlanShut> {

    /**
     * 批量根据唯一键查询
     * 唯一键维度：FACTORY_CODE + MACHINE_CODE + MACHINE_TYPE + MACHINE_STOP_TYPE + BEGIN_DATE
     *
     * @param list 数据列表
     * @return 查询结果
     */
    List<MdmDevicePlanShut> selectByUniqueKeyList(@Param("list") List<MdmDevicePlanShut> list);

    /**
     * 批量根据MES_ID查询设备计划停机
     * 用于MES同步时按MES_ID匹配更新实际完成日期
     *
     * @param list 数据列表（仅需mesId有值）
     * @return 查询结果
     */
    List<MdmDevicePlanShut> selectByMesIdList(@Param("list") List<MdmDevicePlanShut> list);
}
