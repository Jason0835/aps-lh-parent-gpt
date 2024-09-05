package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.EmbryoVersionVo;
import com.zlt.aps.common.engine.domain.TCxMonthPlanAdjust;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 【请填写功能名称】Mapper接口
 * 
 * @author zlt
 * @date 2021-11-10
 */
public interface TCxMonthPlanAdjustMapper 
{
    /**
     * 查询【请填写功能名称】
     * 
     * @param id 【请填写功能名称】ID
     * @return 【请填写功能名称】
     */
    public TCxMonthPlanAdjust selectTCxMonthPlanAdjustById(Long id);

    /**
     * 查询【请填写功能名称】列表
     * 
     * @param tCxMonthPlanAdjust 【请填写功能名称】
     * @return 【请填写功能名称】集合
     */
    public List<TCxMonthPlanAdjust> selectTCxMonthPlanAdjustList(TCxMonthPlanAdjust tCxMonthPlanAdjust);

    public List<TCxMonthPlanAdjust> selectByEmbryoVersionList(@Param("apsVersion") String apsVersion, @Param("list") List<EmbryoVersionVo> list);

    List<TCxMonthPlanAdjust> selectAllByMonthPlanApsVersion(@Param("monthPlanApsVersion") String monthPlanApsVersion);

    /**
     * 新增【请填写功能名称】
     * 
     * @param tCxMonthPlanAdjust 【请填写功能名称】
     * @return 结果
     */
    public int insertTCxMonthPlanAdjust(TCxMonthPlanAdjust tCxMonthPlanAdjust);

    /**
     * 修改【请填写功能名称】
     * 
     * @param tCxMonthPlanAdjust 【请填写功能名称】
     * @return 结果
     */
    public int updateTCxMonthPlanAdjust(TCxMonthPlanAdjust tCxMonthPlanAdjust);

    /**
     * 删除【请填写功能名称】
     * 
     * @param id 【请填写功能名称】ID
     * @return 结果
     */
    public int deleteTCxMonthPlanAdjustById(Long id);

    /**
     * 批量删除【请填写功能名称】
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTCxMonthPlanAdjustByIds(Long[] ids);

    /**
 * 合并操作，如果记录存在则更新，否则新增
 */
    public void mergeSql(List<TCxMonthPlanAdjust> list);
}
