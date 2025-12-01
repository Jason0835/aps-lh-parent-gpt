package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.ProductALevel;
import com.zlt.aps.monthplan.api.domain.vo.ProductALevelVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductALevelMapper.java
 * 描    述：基础数据-SAP-OEE率Mapper接口
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
@Mapper
public interface ProductALevelMapper extends CommBaseMapper<ProductALevel> {

    /**
     * 查询基础数据-SAP-OEE率列表
     *
     * @param docProductALevel 参数
     * @return 结果
     */
    List<ProductALevel> selectDocProductALevelList(ProductALevel docProductALevel);

    /**
     * 根据唯一键查询数据
     *
     * @param uniqueKeyList 唯一键列表
     * @return 结果
     */
    List<ProductALevel> selectByUniqueKeyList(@Param("uniqueKeyList") List<String> uniqueKeyList);

    /**
     * 获取SAP-OEE率列表，关联出品牌
     *
     * @param docProductALevel
     * @return
     */
    List<ProductALevelVo> getProductALevelList(ProductALevel docProductALevel);

    /**
     * 更新备货计划
     * @param ids 选中的数据
     * @param year 年
     * @param month 月
     * @return 结果
     */
    int updateStockUpPlan(@Param("ids") List<Long> ids, @Param("year") Integer year, @Param("month") Integer month);
}
