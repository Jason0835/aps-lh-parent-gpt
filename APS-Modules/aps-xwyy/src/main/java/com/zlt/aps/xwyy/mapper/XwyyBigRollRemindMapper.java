package com.zlt.aps.xwyy.mapper;

import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRemind;

import java.util.List;

/**
 * 帘布大卷原线提醒Mapper接口
 *
 * @author chen
 * @date 2022-04-27
 */
public interface XwyyBigRollRemindMapper {
    /**
     * 查询帘布大卷原线提醒
     *
     * @param id 帘布大卷原线提醒ID
     * @return 帘布大卷原线提醒
     */
    public XwyyBigRollRemind selectXwyyBigRollRemindById(Long id);

    /**
     * 查询帘布大卷原线提醒列表
     *
     * @param xwyyBigRollRemind 帘布大卷原线提醒
     * @return 帘布大卷原线提醒集合
     */
    public List<XwyyBigRollRemind> selectXwyyBigRollRemindList(XwyyBigRollRemind xwyyBigRollRemind);

    /**
     * 新增帘布大卷原线提醒
     *
     * @param xwyyBigRollRemind 帘布大卷原线提醒
     * @return 结果
     */
    public int insertXwyyBigRollRemind(XwyyBigRollRemind xwyyBigRollRemind);

    /**
     * 修改帘布大卷原线提醒
     *
     * @param xwyyBigRollRemind 帘布大卷原线提醒
     * @return 结果
     */
    public int updateXwyyBigRollRemind(XwyyBigRollRemind xwyyBigRollRemind);

    /**
     * 删除帘布大卷原线提醒
     *
     * @param id 帘布大卷原线提醒ID
     * @return 结果
     */
    public int deleteXwyyBigRollRemindById(Long id);

    /**
     * 批量删除帘布大卷原线提醒
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteXwyyBigRollRemindByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<XwyyBigRollRemind> list);

    /**
     * 校验唯一性
     * @return 查询到的记录数
     */
    int checkXwyyBigRollRemindUnique(XwyyBigRollRemind xwyyBigRollRemind);
}
