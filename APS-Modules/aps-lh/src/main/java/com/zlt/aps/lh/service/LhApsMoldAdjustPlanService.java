package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.dto.LhApsMoldAdjustPlanDto;
import com.zlt.aps.lh.api.domain.entity.LhApsMoldAdjustPlan;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 硫化工序模具变动单APSService接口
 * 
 * @author Joran.zhang
 * @date 2022-06-07
 */
public interface LhApsMoldAdjustPlanService
{
    /**
     * 查询硫化工序模具变动单APS
     * 
     * @param id 硫化工序模具变动单APSID
     * @return 硫化工序模具变动单APS
     */
    public LhApsMoldAdjustPlan selectLhApsMoldAdjustPlanById(Long id);

    /**
     * 查询硫化工序模具变动单APS列表
     * 
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS
     * @return 硫化工序模具变动单APS集合
     */
    public List<LhApsMoldAdjustPlan> selectLhApsMoldAdjustPlanList(LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 新增硫化工序模具变动单APS
     * 
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS
     * @return 结果
     */
    @Transactional
    public int insertLhApsMoldAdjustPlan(LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 修改硫化工序模具变动单APS
     * 
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS
     * @return 结果
     */
    @Transactional
    public int updateLhApsMoldAdjustPlan(LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 批量删除硫化工序模具变动单APS
     * 
     * @param ids 需要删除的硫化工序模具变动单APSID
     * @return 结果
     */
    @Transactional
    public int deleteLhApsMoldAdjustPlanByIds(Long[] ids);

    /**
     * 删除硫化工序模具变动单APS信息
     * 
     * @param id 硫化工序模具变动单APSID
     * @return 结果
     */
    @Transactional
    public int deleteLhApsMoldAdjustPlanById(Long id);

    /**
     * 校验硫化工序模具变动单APS唯一性
     */
    public String checkLhApsMoldAdjustPlanUnique(LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 导入硫化工序模具变动单APS数据
     */
    @Transactional
    public AjaxResult importData(List<LhApsMoldAdjustPlan> list, boolean updateSupport, Long importLogId);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByIds(Long[] ids);

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(Long[] ids);

    /**
     * 模具计划发布
     * @param planDate  计划日期
     * @param dataVersion 接口数据版本
     * @param factoryCode 分厂代号
     * @param companyCode  分公司代号
     */
    public AjaxResult publish(long[] ids, Date planDate, String dataVersion, String factoryCode, String companyCode);

    /**
     * 更新指定相关数据记录的发布状态
     *
     * @param dataVersion 数据版本
     * @param ids         排程ID列表
     * @param status      更新的状态
     */
    void updateRelaseStatus(String dataVersion, long[] ids, String status);

    /**
     * 根据ids更改执行状态
     * @param lhApsMoldAdjustPlan ids、要更改的状态
     * @return 结果
     */
    public AjaxResult changeExecute(LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 新增硫化工序模具变动单APS主子表
     *
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS主子表
     * @return 结果
     */
    public AjaxResult addSubData(LhApsMoldAdjustPlanDto dto);
}
