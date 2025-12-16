package com.zlt.aps.monthplan.factory.mapper;

import java.util.List;

import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanProdFinal;
import org.apache.ibatis.annotations.Mapper;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanProdFinalMapper.java
 * 描    述：工厂月生产计划-最终排产计划定稿Mapper接口
 *@author yelq
 *@date 2025-12-16
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Mapper
public interface MpMonthPlanProdFinalEntityMapper extends IBaseMapper<MpMonthPlanProdFinal>
{
    /**
     * 查询工厂月生产计划-最终排产计划定稿
     * 
     * @param id 工厂月生产计划-最终排产计划定稿主键
     * @return 工厂月生产计划-最终排产计划定稿
     */
    MpMonthPlanProdFinal selectMpMonthPlanProdFinalById(Integer id);

    /**
     * 查询工厂月生产计划-最终排产计划定稿列表
     * 
     * @param mpMonthPlanProdFinal 工厂月生产计划-最终排产计划定稿
     * @return 工厂月生产计划-最终排产计划定稿集合
     */
    List<MpMonthPlanProdFinal> selectMpMonthPlanProdFinalList(MpMonthPlanProdFinal mpMonthPlanProdFinal);

    /**
     * 批量查询工厂月生产计划-最终排产计划定稿列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 工厂月生产计划-最终排产计划定稿集合
     */
    List<MpMonthPlanProdFinal> selectMpMonthPlanProdFinalByIds(List<Integer> ids);

    /**
     * 删除工厂月生产计划-最终排产计划定稿
     * 
     * @param id 工厂月生产计划-最终排产计划定稿主键
     * @return 结果
     */
    int deleteMpMonthPlanProdFinalById(Integer id);

    /**
     * 批量删除工厂月生产计划-最终排产计划定稿
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMpMonthPlanProdFinalByIds(Integer[] ids);
}
