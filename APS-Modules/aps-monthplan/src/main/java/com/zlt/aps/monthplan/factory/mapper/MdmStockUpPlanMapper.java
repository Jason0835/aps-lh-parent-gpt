package com.zlt.aps.monthplan.factory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmStockUpPlan;
import com.zlt.aps.monthplan.api.domain.vo.MdmStockUpPlanVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmStockUpPlanMapper.java
 * 描    述：备货计划Mapper接口
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-18
 */
@Mapper
public interface MdmStockUpPlanMapper extends BaseMapper<MdmStockUpPlan> {

    /**
     * 查询备货计划列表
     *
     * @param mdmStockUpPlan 备货计划
     * @return 备货计划集合
     */
    List<MdmStockUpPlanVo> selectMdmStockUpPlanList(MdmStockUpPlanVo mdmStockUpPlan);

    /**
     * 删除对应年月、轮胎类型的备货量数据
     *
     * @param year     年
     * @param month    月
     * @param tireType 轮胎类型
     * @return 结果数
     */
    int deleteByParams(@Param("year") int year, @Param("month") int month, @Param("tireType") String tireType);

    /**
     * 获取备货计划，最开始的月均销量月份数
     *
     * @param mdmStockUpPlan 查询条件
     * @return
     */
    Integer getAverageType(MdmStockUpPlanVo mdmStockUpPlan);

    /**
     * 校验备货计划是否存在
     *
     * @param mdmStockUpPlan
     * @return
     */
    int existByKey(MdmStockUpPlanVo mdmStockUpPlan);
}
