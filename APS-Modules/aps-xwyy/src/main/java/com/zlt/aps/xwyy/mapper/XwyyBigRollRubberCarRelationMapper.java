package com.zlt.aps.xwyy.mapper;

import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRubberCarRelation;

import java.util.List;

/**
 * 帘布大卷胶料号车数关系Mapper接口
 *
 * @author Joran.Zhang
 * @date 2022-05-10
 */
public interface XwyyBigRollRubberCarRelationMapper {
    /**
     * 查询帘布大卷胶料号车数关系
     *
     * @param id 帘布大卷原线胶料号车数关系ID
     * @return 帘布大卷原线胶料号车数关系
     */
    public XwyyBigRollRubberCarRelation selectXwyyBigRollRubberCarRelationById(Long id);

    /**
     * 查询帘布大卷原线提醒列表
     *
     * @param XwyyBigRollRubberCarRelation 帘布大卷原线提醒
     * @return 帘布大卷原线提醒集合
     */
    public List<XwyyBigRollRubberCarRelation> selectXwyyBigRollRubberCarRelationList(XwyyBigRollRubberCarRelation XwyyBigRollRubberCarRelation);

    /**
     * 新增帘布大卷原线提醒
     *
     * @param XwyyBigRollRubberCarRelation 帘布大卷原线提醒
     * @return 结果
     */
    public int insertXwyyBigRollRubberCarRelation(XwyyBigRollRubberCarRelation XwyyBigRollRubberCarRelation);

    /**
     * 修改帘布大卷原线提醒
     *
     * @param XwyyBigRollRubberCarRelation 帘布大卷原线提醒
     * @return 结果
     */
    public int updateXwyyBigRollRubberCarRelation(XwyyBigRollRubberCarRelation XwyyBigRollRubberCarRelation);

    /**
     * 删除帘布大卷原线提醒
     *
     * @param id 帘布大卷原线提醒ID
     * @return 结果
     */
    public int deleteXwyyBigRollRubberCarRelationById(Long id);

    /**
     * 批量删除帘布大卷原线提醒
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteXwyyBigRollRubberCarRelationByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<XwyyBigRollRubberCarRelation> list);

    /**
     * 校验唯一性
     * @return 查询到的记录数
     */
    int checkXwyyBigRollRubberCarRelationUnique(XwyyBigRollRubberCarRelation XwyyBigRollRubberCarRelation);

    public XwyyBigRollRubberCarRelation selectByBigRollCode(XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation);
}
