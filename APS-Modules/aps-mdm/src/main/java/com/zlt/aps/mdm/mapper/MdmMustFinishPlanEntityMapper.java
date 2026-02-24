package com.zlt.aps.mdm.mapper;

import com.zlt.aps.mdm.api.domain.entity.MdmMustFinishPlan;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMustFinishPlanMapper.java
 * 描    述：必须保证的客户月计划Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */
@Mapper
public interface MdmMustFinishPlanEntityMapper extends CommBaseMapper<MdmMustFinishPlan> {

}
