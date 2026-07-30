package com.zlt.aps.gsq.service;

import com.zlt.aps.gsq.entity.GsqParams;
import com.zlt.bill.common.service.IDocService;

import java.util.Map;

/**
 * 钢丝圈排程参数配置 Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface GsqParamsService extends IDocService<GsqParams> {

    /**
     * 根据参数编码和工厂编号查询参数
     *
     * @param paramCode   参数编码
     * @param factoryCode 工厂编号
     * @return 参数实体
     */
    GsqParams selectOneByParamCode(String paramCode, String factoryCode);

    /**
     * 查询指定工厂的所有参数，返回参数编码到参数值的映射
     *
     * @param factoryCode 工厂编号
     * @return 参数编码-参数值映射
     */
    Map<String, String> listGsqParams(String factoryCode);
}
