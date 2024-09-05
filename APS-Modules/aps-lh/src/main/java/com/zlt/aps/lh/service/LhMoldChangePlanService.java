package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;

import java.util.Date;
import java.util.List;

/**
 * 模具变动单Service接口
 *
 * @author zlt
 * @date 2021-06-17
 */
public interface LhMoldChangePlanService {
    /**
     * 查询模具变动单
     *
     * @param id 模具变动单ID
     * @return 模具变动单
     */
    public LhMoldChangePlan selectLhMoldChangePlanById(Long id);

    /**
     * 查询模具变动单列表
     *
     * @param lhMoldChangePlan 模具变动单
     * @return 模具变动单集合
     */
    public List<LhMoldChangePlan> selectLhMoldChangePlanList(LhMoldChangePlan lhMoldChangePlan);

    /**
     * 新增模具变动单
     *
     * @param lhMoldChangePlan 模具变动单
     * @return 结果
     */
    public AjaxResult insertLhMoldChangePlan(LhMoldChangePlan lhMoldChangePlan);

    /**
     * 修改模具变动单
     *
     * @param lhMoldChangePlan 模具变动单
     * @return 结果
     */
    public int updateLhMoldChangePlan(LhMoldChangePlan lhMoldChangePlan);

    /**
     * 批量删除模具变动单
     *
     * @param ids 需要删除的模具变动单ID
     * @return 结果
     */
    public int deleteLhMoldChangePlanByIds(Long[] ids);

    /**
     * 删除模具变动单信息
     *
     * @param id 模具变动单ID
     * @return 结果
     */
    public int deleteLhMoldChangePlanById(Long id);

    /**
     * 导入数据
     */
    AjaxResult importData(List<LhMoldChangePlan> list, boolean updateSupport, Long importLogId);

	/**
	 * 发布
	 * @param ids	待发布模具变动单id
	 * @param scheduleDate	排程日期
	 * @return
	 */
	public AjaxResult publish(long[] ids, Date scheduleDate);
}
