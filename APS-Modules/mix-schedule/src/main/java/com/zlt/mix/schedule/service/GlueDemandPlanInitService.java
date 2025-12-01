package com.zlt.mix.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;

import java.util.List;

/**
 * 分厂胶料需求计划（初始表）Service接口
 * 
 * @author Gim
 * @date 2022-04-05
 */
public interface GlueDemandPlanInitService  extends IService<GlueDemandPlanInit>
{
    /**
     * 查询分厂胶料需求计划（初始表）列表
     * 
     * @param glueDemandPlanInit 分厂胶料需求计划（初始表）
     * @return 分厂胶料需求计划（初始表）集合
     */
    List<GlueDemandPlanInit> selectGlueDemandPlanInitList(GlueDemandPlanInit glueDemandPlanInit);

}
