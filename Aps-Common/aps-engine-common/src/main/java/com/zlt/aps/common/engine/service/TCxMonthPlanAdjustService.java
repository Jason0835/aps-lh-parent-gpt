package com.zlt.aps.common.engine.service;

import java.util.List;

import com.zlt.aps.common.engine.domain.EmbryoVersionVo;
import com.zlt.aps.common.engine.domain.TCxMonthPlanAdjust;
import org.springframework.transaction.annotation.Transactional;

/**
 * 【请填写功能名称】Service接口
 * 
 * @author zlt
 * @date 2021-11-10
 */
public interface TCxMonthPlanAdjustService
{
    /**
     * 查询【请填写功能名称】
     * 
     * @param id 【请填写功能名称】ID
     * @return 【请填写功能名称】
     */
    public TCxMonthPlanAdjust selectTCxMonthPlanAdjustById(Long id);

    public List<TCxMonthPlanAdjust> selectTCxMonthPlanAdjustList(TCxMonthPlanAdjust tCxMonthPlanAdjust);

    /**
     * 多来源汇总成一条，只按embryoCode和bomDataVersion分组
     * @param apsVersion
     * @param list
     * @return
     */
    public List<TCxMonthPlanAdjust> selectByEmbryoVersionList(String apsVersion, List<EmbryoVersionVo> list);

    public List<TCxMonthPlanAdjust> selectAllByApsVersionList(String apsVersion);

    /**
     * 新增【请填写功能名称】
     * 
     * @param tCxMonthPlanAdjust 【请填写功能名称】
     * @return 结果
     */
    @Transactional
    public int insertTCxMonthPlanAdjust(TCxMonthPlanAdjust tCxMonthPlanAdjust);

    /**
     * 修改【请填写功能名称】
     * 
     * @param tCxMonthPlanAdjust 【请填写功能名称】
     * @return 结果
     */
    @Transactional
    public int updateTCxMonthPlanAdjust(TCxMonthPlanAdjust tCxMonthPlanAdjust);

    /**
     * 批量删除【请填写功能名称】
     * 
     * @param ids 需要删除的【请填写功能名称】ID
     * @return 结果
     */
    @Transactional
    public int deleteTCxMonthPlanAdjustByIds(Long[] ids);

    /**
     * 删除【请填写功能名称】信息
     * 
     * @param id 【请填写功能名称】ID
     * @return 结果
     */
    @Transactional
    public int deleteTCxMonthPlanAdjustById(Long id);

    /**
     * 校验【请填写功能名称】唯一性
     */
    public String checkTCxMonthPlanAdjustUnique(TCxMonthPlanAdjust tCxMonthPlanAdjust);

    void mergeSql(List<TCxMonthPlanAdjust> list);
}
