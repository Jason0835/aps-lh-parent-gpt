package com.zlt.aps.monthplan.mdm.mapper;

import com.zlt.aps.monthplan.api.domain.entity.EstimateExceedShort;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：EstimateExceedShortMapper.java
 * 描    述：预计超欠产Mapper接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-18
 */
@Mapper
public interface EstimateExceedShortMapper extends CommBaseMapper<EstimateExceedShort> {

    /**
     * 生成预计超欠产
     *
     * @param username      创建人
     * @param factoryCode   分厂编号
     * @param generateYear  生成年
     * @param generateMonth 生成月
     * @param sumFieldList  计划汇总字段
     * @return 结果数据
     */
    @Deprecated
    int generateEstimateExceedShort(@Param("username") String username,
                                    @Param("factoryCode") String factoryCode,
                                    @Param("generateYear") Integer generateYear,
                                    @Param("generateMonth") Integer generateMonth,
                                    @Param("sumFieldList") List<String> sumFieldList);

    /**
     * 查询月度计划排产量
     *
     * @param factoryCode   分厂编号
     * @param generateYear  生成年
     * @param generateMonth 生成月
     * @param sumFieldList  计划汇总字段
     * @return 月度计划排产量
     */
    List<EstimateExceedShort> selectMonthPlanList(@Param("factoryCode") String factoryCode,
                                                  @Param("generateYear") Integer generateYear,
                                                  @Param("generateMonth") Integer generateMonth,
                                                  @Param("sumFieldList") List<String> sumFieldList);

    /**
     * 查询实际生产库存量
     *
     * @param factoryCode   分厂编号
     * @param generateYear  生成年
     * @param generateMonth 生成月
     * @return 实际生产库存量
     */
    List<EstimateExceedShort> selectSurplusPlanList(@Param("factoryCode") String factoryCode,
                                                    @Param("generateYear") Integer generateYear,
                                                    @Param("generateMonth") Integer generateMonth);
}
