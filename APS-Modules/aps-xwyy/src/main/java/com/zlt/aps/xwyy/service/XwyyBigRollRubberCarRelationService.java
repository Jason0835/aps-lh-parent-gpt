package com.zlt.aps.xwyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRubberCarRelation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 帘布大卷原线胶料号车数关系Service接口
 *
 * @author Joran.Zhang
 * @date 2022-05-10
 */
public interface XwyyBigRollRubberCarRelationService {
    /**
     * 查询帘布大卷原线胶料号车数关系
     *
     * @param id 帘布大卷原线胶料号车数关系ID
     * @return 帘布大卷原线胶料号车数关系
     */
    public XwyyBigRollRubberCarRelation selectXwyyBigRollRubberCarRelationById(Long id);

    /**
     * 查询帘布大卷原线胶料号车数关系列表
     *
     * @param XwyyBigRollRubberCarRelation 帘布大卷原线胶料号车数关系
     * @return 帘布大卷原线胶料号车数关系集合
     */
    public List<XwyyBigRollRubberCarRelation> selectXwyyBigRollRubberCarRelationList(XwyyBigRollRubberCarRelation XwyyBigRollRubberCarRelation);

    /**
     * 新增帘布大卷原线胶料号车数关系
     *
     * @param XwyyBigRollRubberCarRelation 帘布大卷原线胶料号车数关系
     * @return 结果
     */
    @Transactional
    public int insertXwyyBigRollRubberCarRelation(XwyyBigRollRubberCarRelation XwyyBigRollRubberCarRelation);

    /**
     * 修改帘布大卷原线胶料号车数关系
     *
     * @param XwyyBigRollRubberCarRelation 帘布大卷原线胶料号车数关系
     * @return 结果
     */
    @Transactional
    public int updateXwyyBigRollRubberCarRelation(XwyyBigRollRubberCarRelation XwyyBigRollRubberCarRelation);

    /**
     * 批量删除帘布大卷原线胶料号车数关系
     *
     * @param ids 需要删除的帘布大卷原线胶料号车数关系ID
     * @return 结果
     */
    @Transactional
    public int deleteXwyyBigRollRubberCarRelationByIds(Long[] ids);

    /**
     * 删除帘布大卷原线胶料号车数关系信息
     *
     * @param id 帘布大卷原线胶料号车数关系ID
     * @return 结果
     */
    @Transactional
    public int deleteXwyyBigRollRubberCarRelationById(Long id);

    /**
     * 校验帘布大卷原线胶料号车数关系唯一性
     */
    public String checkXwyyBigRollRubberCarRelationUnique(XwyyBigRollRubberCarRelation XwyyBigRollRubberCarRelation);

    /**
     * 导入帘布大卷原线胶料号车数关系数据
     */
    @Transactional
    public AjaxResult importData(List<XwyyBigRollRubberCarRelation> list, boolean updateSupport, Long importLogId);

    XwyyBigRollRubberCarRelation selectByBigRollCode(XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation);
}
