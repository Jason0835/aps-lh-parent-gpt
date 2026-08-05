package com.zlt.aps.gsq.service;

import com.zlt.aps.gsq.api.domain.entity.GsqParams;
import com.zlt.bill.common.service.IDocService;

import java.util.Map;

/**
 * 钢丝圈参数信息Service接口（对齐胎圈 TqParamsService）
 *
 * @author zlt
 * @version 1.0
 * @date 2025-12-12
 */
public interface GsqParamsService extends IDocService<GsqParams> {

    /**
     * 根据参数编码+工厂编码查询参数配置
     *
     * @param paramCode   参数编码
     * @param factoryCode 工厂编码（可为空）
     * @return 参数配置
     */
    GsqParams selectOneByParamCode(String paramCode, String factoryCode);

    /**
     * 查询钢丝圈参数配置（按工厂编码），返回 参数编码→参数值 映射
     *
     * @param factoryCode 工厂编码
     * @return 参数映射
     */
    Map<String, String> listGsqParams(String factoryCode);
}