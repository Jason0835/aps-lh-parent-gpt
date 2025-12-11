package com.zlt.aps.maindata.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.tlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.maindata.domain.vo.SizeCapacityParamVo;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.vo.FactoryParamVo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryParamService.java
 * 描    述：IFactoryParamService系统参数（排产设定）后端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
public interface IFactoryParamService extends IService<FactoryParam> {

    /**
     * 获取排产设定集合
     *
     * @param entity
     * @return
     */
    List<FactoryParam> getFacParamByList(FactoryParam entity);

    /**
     * 复制分厂排产设定
     *
     * @param factoryParamVo
     * @return
     */
    AjaxResult copy(FactoryParamVo factoryParamVo);

    /**
     * 从参数模板更新指定分厂系统参数
     *
     * @param factoryCode
     * @param productTypeCode
     */
    void syncSysParamtersFromTemplate(String factoryCode, String productTypeCode);

    /**
     * 查询唯一的分厂系统参数
     *
     * @param factoryParam
     * @return
     */
    FactoryParam getFacParamSingle(FactoryParam factoryParam);

    /**
     * 得到单条硫化间隔增加时间
     * 单位-秒
     *
     * @param factoryCode 分厂
     * @return
     */
    BigDecimal getSingleAddCuringTime(String factoryCode);

    /**
     * 换规格需要消耗的产能
     * 单位-秒
     *
     * @param factoryCode 分厂
     * @return
     */
    BigDecimal getChangeProductConsumeTime(String factoryCode);

    /**
     * 获取一天最大的硫化时间
     * 单位-秒
     *
     * @param factoryCode
     * @return
     */
    BigDecimal getDayMaxCuringTime(String factoryCode);

    /**
     * 获取夏冬季切换配置
     *
     * @param factoryCode 分厂
     * @return
     */
    Map<String, Integer> getChangeSummerMonth(String factoryCode);

    /**
     * 获取试制，量试的数量，并转换成硫化时间
     *
     * @param factoryCode
     * @return
     */
    Integer getInformalConstructionCuringTime(String factoryCode);

    /**
     * 获取不进行备货的品牌计划-主要为外贸贴牌品牌
     *
     * @param factoryCode 工厂编码
     * @return
     */
    Set<String> getNoStockUpPlanBrand(String factoryCode);

    /**
     * 获取外销贴牌品牌配置
     *
     * @param factoryCode
     * @return
     */
    Set<String> getForeignOemBrand(String factoryCode);

    /**
     * 是否开启无订单备货计划
     *
     * @param factoryCode
     * @return
     */
    boolean isOpenNoSubmitStockUp(String factoryCode);

    /**
     * 获取内销的备货方式是否采用特定方式
     * Y表示另外的备货方式，N表示与外销、OE一致
     *
     * @param factoryCode 工厂编码
     * @return
     */
    String getDomesticStockUpType(String factoryCode);

    /**
     * 获取 备货计划需要提前的月份参数值
     *
     * @param factoryCode 工厂编码
     * @return
     */
    Integer getStockUpLastMonth(String factoryCode);

    /**
     * 获取工厂的月份起始周期配置
     * 在2~28值之间则表示非自然月，否则为自然月
     *
     * @param factoryCode 工厂编码
     * @param productType 业务类型 TBR 全钢 PCR 半钢
     * @return
     */
    Integer getMonthStartDay(String factoryCode, ProductTypeEnum productType);

    /**
     * 获取分厂的寸口产能分配参数
     *
     * @param factoryCode 工厂编码
     * @return
     */
    SizeCapacityParamVo getSizeCapacityAllocationParam(String factoryCode);
}
