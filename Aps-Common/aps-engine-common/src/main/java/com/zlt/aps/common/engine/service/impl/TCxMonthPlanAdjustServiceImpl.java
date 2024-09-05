package com.zlt.aps.common.engine.service.impl;


import java.util.ArrayList;
import java.util.List;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.EmbryoVersionVo;
import com.zlt.aps.common.engine.domain.TCxMonthPlanAdjust;
import com.zlt.aps.common.engine.mapper.TCxMonthPlanAdjustMapper;
import com.zlt.aps.common.engine.service.TCxMonthPlanAdjustService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.UserConstants;



/**
 * 成型计划修正表Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-10
 */
@Service
public class TCxMonthPlanAdjustServiceImpl implements TCxMonthPlanAdjustService
{
    @Autowired
    private TCxMonthPlanAdjustMapper tCxMonthPlanAdjustMapper;

    /**
     * 查询成型计划修正表
     * 
     * @param id 成型计划修正表ID
     * @return 成型计划修正表
     */
    @Override
    public TCxMonthPlanAdjust selectTCxMonthPlanAdjustById(Long id)
    {
        return tCxMonthPlanAdjustMapper.selectTCxMonthPlanAdjustById(id);
    }

    /**
     * 查询成型计划修正表列表
     * 
     * @param tCxMonthPlanAdjust 成型计划修正表
     * @return 成型计划修正表
     */
    @Override
    public List<TCxMonthPlanAdjust> selectTCxMonthPlanAdjustList(TCxMonthPlanAdjust tCxMonthPlanAdjust)
    {
        return tCxMonthPlanAdjustMapper.selectTCxMonthPlanAdjustList(tCxMonthPlanAdjust);
    }

    @Override
    public List<TCxMonthPlanAdjust> selectByEmbryoVersionList(String apsVersion, List<EmbryoVersionVo> list) {
        if (CollectionUtil.isEmpty(list) || StringUtils.isBlank(apsVersion)) {
            return new ArrayList<>();
        }
        return tCxMonthPlanAdjustMapper.selectByEmbryoVersionList(apsVersion, list);
    }

    @Override
    public List<TCxMonthPlanAdjust> selectAllByApsVersionList(String apsVersion) {
        if (StringUtils.isBlank(apsVersion)) {
            return new ArrayList<>();
        }
        return tCxMonthPlanAdjustMapper.selectAllByMonthPlanApsVersion(apsVersion);
    }

    /**
     * 新增成型计划修正表
     * 
     * @param tCxMonthPlanAdjust 成型计划修正表
     * @return 结果
     */
    @Override
    public int insertTCxMonthPlanAdjust(TCxMonthPlanAdjust tCxMonthPlanAdjust)
    {
        tCxMonthPlanAdjust.setBaseVale(null);
        return tCxMonthPlanAdjustMapper.insertTCxMonthPlanAdjust(tCxMonthPlanAdjust);
    }

    /**
     * 修改成型计划修正表
     * 
     * @param tCxMonthPlanAdjust 成型计划修正表
     * @return 结果
     */
    @Override
    public int updateTCxMonthPlanAdjust(TCxMonthPlanAdjust tCxMonthPlanAdjust)
    {
        tCxMonthPlanAdjust.setBaseVale(tCxMonthPlanAdjust.getId());
        return tCxMonthPlanAdjustMapper.updateTCxMonthPlanAdjust(tCxMonthPlanAdjust);
    }

    /**
     * 批量删除成型计划修正表
     * 
     * @param ids 需要删除的成型计划修正表ID
     * @return 结果
     */
    @Override
    public int deleteTCxMonthPlanAdjustByIds(Long[] ids)
    {
        return tCxMonthPlanAdjustMapper.deleteTCxMonthPlanAdjustByIds(ids);
    }

    /**
     * 删除成型计划修正表信息
     * 
     * @param id 成型计划修正表ID
     * @return 结果
     */
    @Override
    public int deleteTCxMonthPlanAdjustById(Long id)
    {
        return tCxMonthPlanAdjustMapper.deleteTCxMonthPlanAdjustById(id);
    }

    /**
     * 校验成型计划修正表唯一性
     */
    @Override
    public String checkTCxMonthPlanAdjustUnique(TCxMonthPlanAdjust tCxMonthPlanAdjust) {
        if (tCxMonthPlanAdjust == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<TCxMonthPlanAdjust> list = tCxMonthPlanAdjustMapper.selectTCxMonthPlanAdjustList(tCxMonthPlanAdjust);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public void mergeSql(List<TCxMonthPlanAdjust> list) {
        tCxMonthPlanAdjustMapper.mergeSql(list);
    }


}
