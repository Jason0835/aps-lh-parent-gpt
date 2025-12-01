package com.zlt.aps.monthplan.factory.service;

/**
 * 生成相关数据
 * 1、生成成型/硫化机正在生成品种
 * 2、生成预计超欠产
 */
public interface IMdmProductionGenerateService {

    /**
     * 根据指定年月上个月的数据，生成分厂/硫化成型正在生成产品
     *
     * @param generateYear  生成年
     * @param generateMonth 生成月
     * @param factoryCode   生成分厂
     * @return 结果数
     */
    int generateProductData(Integer generateYear, Integer generateMonth, String factoryCode);

    /**
     * 根据指定年月和当前所在日，生成预计超欠产
     *
     * @param generateYear  生成年
     * @param generateMonth 生成月
     * @param factoryCode   分厂
     * @return 结果数
     */
    int generateEstimateExceedShort(Integer generateYear, Integer generateMonth, String factoryCode);
}
