package com.zlt.aps.mp.api.remoteService;

import com.zlt.aps.mp.api.domain.entity.ProductStockMonth;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductStockMonthRemoteService.java
 * 描    述：IProductStockMonthRemoteService物料月库存信息前端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-03-12
 */
@FeignClient(contextId = "IProductStockMonthRemoteApiService", name = "${remoteApi.value.monthplan:aps-monthplan}")
public interface IProductStockMonthRemoteApiService {

    /**
     * 查询列表
     *
     * @param queryVo 查询参数
     * @return 结果
     */
    @ApiOperation("查询列表")
    @PostMapping("/monthStock/selectList")
    List<ProductStockMonth> selectList(@RequestBody ProductStockMonth queryVo);

}
