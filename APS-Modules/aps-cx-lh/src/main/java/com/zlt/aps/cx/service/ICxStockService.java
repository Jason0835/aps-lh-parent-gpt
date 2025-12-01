package com.zlt.aps.cx.service;


import com.zlt.aps.cxlh.cx.api.domain.entity.CxStock;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxStockService.java
 * 描    述：ICxStockService成型库存信息后端接口
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ICxStockService  extends IDocService<CxStock>{


    /**
     * 按照日期查询成型工序胎胚库存
     * @param scheduleDate 查询的库存日期
     * @return List<CxStock> 有效库存列表
     */
    public List<CxStock> queryStockByDate(Date scheduleDate);

    /**
     * 按照日期查询成型工序胎胚库存
     * @param scheduleDate 查询的库存日期
     * @return List<CxStock> 有效库存列表
     */
    public CxStock queryStockByEmbryo(Date scheduleDate,String embryo);

}
