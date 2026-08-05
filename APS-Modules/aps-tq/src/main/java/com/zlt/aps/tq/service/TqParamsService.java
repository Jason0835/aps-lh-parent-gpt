package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqParams;
import com.zlt.bill.common.service.IDocService;

import java.util.Map;

/**
 * 胎圈排程参数配置 Service接口（对齐胎面 ITmParamsService）
 *
 * @author zlt
 * @version 1.0
 * @date 2025-12-12
 */
public interface TqParamsService extends IDocService<TqParams> {

    /**
     * 按参数编码+工厂编码查询参数（工厂编码为空时忽略）
     *
     * @param paramCode   参数编码
     * @param factoryCode 工厂编码
     * @return 参数
     */
    TqParams selectOneByParamCode(String paramCode, String factoryCode);

    /**
     * 查询某工厂的全部参数映射（paramCode -> paramValue）
     *
     * @param factoryCode 工厂编码
     * @return 参数映射
     */
    Map<String, String> listTqParams(String factoryCode);
}