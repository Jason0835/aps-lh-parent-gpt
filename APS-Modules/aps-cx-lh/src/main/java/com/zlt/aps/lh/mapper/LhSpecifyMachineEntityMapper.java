package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhSpecifyMachineMapper.java
 * 描    述：硫化定点机台信息Mapper接口
 *@author zlt
 *@date 2025-03-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface LhSpecifyMachineEntityMapper extends CommBaseMapper<LhSpecifyMachine> {


    /**
     * 根据工厂编号和规格代码查询
     * @param factoryCode
     * @param specCodes
     * @return
     */
    List<LhSpecifyMachine> queryByFactoryCodeAndSpecCodes(@Param("factoryCode") String factoryCode,
                                                          @Param("specCodes") List<String> specCodes);

}
