package com.zlt.aps.monthplan.factory.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpStructureAllocationService.java
 * 描    述：IMpStructureAllocationService排产过程_结构排产后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-29
 */
public interface IMpStructureAllocationService extends IDocService<MpStructureAllocation> {
    /**
     * 根据查询条件，获取结构排产信息
     *
     * @param param
     * @return
     */
    List<MpStructureAllocation> getDataList(MpStructureAllocation param);
}
