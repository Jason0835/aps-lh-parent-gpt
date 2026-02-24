package com.zlt.aps.mdm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.mdm.api.domain.entity.MdmCustomerInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmCustomerInfoService.java
 * 描    述：IMdmCustomerInfoService客户信息后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-04
 */
public interface IMdmCustomerInfoService extends IDocService<MdmCustomerInfo> {

    /**
     * 根据指定的查询条件，查询符合条件的客户信息列表。
     *
     * @param wrapper 查询条件封装对象，用于构建查询条件。该对象包含查询字段、排序规则、分页信息等。
     * @return 返回符合条件的客户信息列表。如果未找到符合条件的记录，则返回空列表。
     */
    List<MdmCustomerInfo> selectList(QueryWrapper<MdmCustomerInfo> wrapper);

}
