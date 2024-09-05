package com.zlt.aps.lh.service;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixTakePlan;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 支领计划Service接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixTakePlanService
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
    @Transactional
    public int insertMixTakePlan(MixTakePlan mixTakePlan);

    /**
     * 修改支领计划
     * 
     * @param mixTakePlan 支领计划
     * @return 结果
     */
    @Transactional
    public int updateMixTakePlan(MixTakePlan mixTakePlan);

    /**
     * 批量删除支领计划
     * 
     * @param ids 需要删除的支领计划ID
     * @return 结果
     */
    @Transactional
    public int deleteMixTakePlanByIds(Long[] ids);

    /**
     * 删除支领计划信息
     * 
     * @param id 支领计划ID
     * @return 结果
     */
    @Transactional
    public int deleteMixTakePlanById(Long id);

    /**
     * 校验支领计划唯一性
     */
    public String checkMixTakePlanUnique(MixTakePlan mixTakePlan);

    /**
     * 导入支领计划数据
     */
    @Transactional
    public AjaxResult importData(List<MixTakePlan> list, boolean updateSupport, Long importLogId);
}
