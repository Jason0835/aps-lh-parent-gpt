package com.zlt.aps.xwyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRemind;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 帘布大卷原线提醒Service接口
 *
 * @author chen
 * @date 2022-04-27
 */
public interface XwyyBigRollRemindService {
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
    @Transactional
    public int insertXwyyBigRollRemind(XwyyBigRollRemind xwyyBigRollRemind);

    /**
     * 修改帘布大卷原线提醒
     *
     * @param xwyyBigRollRemind 帘布大卷原线提醒
     * @return 结果
     */
    @Transactional
    public int updateXwyyBigRollRemind(XwyyBigRollRemind xwyyBigRollRemind);

    /**
     * 批量删除帘布大卷原线提醒
     *
     * @param ids 需要删除的帘布大卷原线提醒ID
     * @return 结果
     */
    @Transactional
    public int deleteXwyyBigRollRemindByIds(Long[] ids);

    /**
     * 删除帘布大卷原线提醒信息
     *
     * @param id 帘布大卷原线提醒ID
     * @return 结果
     */
    @Transactional
    public int deleteXwyyBigRollRemindById(Long id);

    /**
     * 校验帘布大卷原线提醒唯一性
     */
    public String checkXwyyBigRollRemindUnique(XwyyBigRollRemind xwyyBigRollRemind);

    /**
     * 导入帘布大卷原线提醒数据
     */
    @Transactional
    public AjaxResult importData(List<XwyyBigRollRemind> list, boolean updateSupport, Long importLogId);
}
