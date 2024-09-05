package com.zlt.aps.cx.mapper;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxEstimateExceedShort;

/**
 * 排程给主计划的预计超欠产Mapper接口
 * 
 * @author zlt
 * @date 2021-09-17
 */
public interface CxEstimateExceedShortMapper 
{
    /**
     * 查询排程给主计划的预计超欠产
     * 
     * @param id 排程给主计划的预计超欠产ID
     * @return 排程给主计划的预计超欠产
     */
    public CxEstimateExceedShort selectCxEstimateExceedShortById(Long id);

    /**
     * 查询排程给主计划的预计超欠产列表
     * 
     * @param cxEstimateExceedShort 排程给主计划的预计超欠产
     * @return 排程给主计划的预计超欠产集合
     */
    public List<CxEstimateExceedShort> selectCxEstimateExceedShortList(CxEstimateExceedShort cxEstimateExceedShort);

    /**
     * 新增排程给主计划的预计超欠产
     * 
     * @param cxEstimateExceedShort 排程给主计划的预计超欠产
     * @return 结果
     */
    public int insertCxEstimateExceedShort(CxEstimateExceedShort cxEstimateExceedShort);

    /**
     * 修改排程给主计划的预计超欠产
     * 
     * @param cxEstimateExceedShort 排程给主计划的预计超欠产
     * @return 结果
     */
    public int updateCxEstimateExceedShort(CxEstimateExceedShort cxEstimateExceedShort);

    /**
     * 删除排程给主计划的预计超欠产
     * 
     * @param id 排程给主计划的预计超欠产ID
     * @return 结果
     */
    public int deleteCxEstimateExceedShortById(Long id);

    /**
     * 批量删除排程给主计划的预计超欠产
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxEstimateExceedShortByIds(Long[] ids);

    /**
     * 删除对应月度计划
     */
    public int deleteByMonth(CxEstimateExceedShort cxEstimateExceedShort);

}
