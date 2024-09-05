package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixTakePlan;

/**
 * 支领计划Mapper接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixTakePlanMapper 
{
    /**
     * 查询支领计划
     * 
     * @param id 支领计划ID
     * @return 支领计划
     */
    public MixTakePlan selectMixTakePlanById(Long id);

    /**
     * 查询支领计划列表
     * 
     * @param mixTakePlan 支领计划
     * @return 支领计划集合
     */
    public List<MixTakePlan> selectMixTakePlanList(MixTakePlan mixTakePlan);

    /**
     * 新增支领计划
     * 
     * @param mixTakePlan 支领计划
     * @return 结果
     */
    public int insertMixTakePlan(MixTakePlan mixTakePlan);

    /**
     * 修改支领计划
     * 
     * @param mixTakePlan 支领计划
     * @return 结果
     */
    public int updateMixTakePlan(MixTakePlan mixTakePlan);

    /**
     * 删除支领计划
     * 
     * @param id 支领计划ID
     * @return 结果
     */
    public int deleteMixTakePlanById(Long id);

    /**
     * 批量删除支领计划
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixTakePlanByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<MixTakePlan> list);
}
