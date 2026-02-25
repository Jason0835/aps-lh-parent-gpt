package com.zlt.aps.maindata.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.mp.api.domain.entity.MdmMsgTemplateUserRel;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMsgTemplateUserRelService.java
 * 描    述：IMdmMsgTemplateUserRelService消息模板关联用户后端接口
 *
 * @author hc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hc
 * 修改内容：...
 * @date 2026-01-28
 */
public interface IMdmMsgTemplateUserRelService extends IService<MdmMsgTemplateUserRel> {
    /**
     * 查询消息模板关联用户
     *
     * @param id 消息模板关联用户主键
     * @return 消息模板关联用户
     */
    public MdmMsgTemplateUserRel selectMdmMsgTemplateUserRelById(Long id);

    /**
     * 查询消息模板关联用户列表
     *
     * @param mdmMsgTemplateUserRel 消息模板关联用户
     * @return 消息模板关联用户集合
     */
    public List<MdmMsgTemplateUserRel> selectMdmMsgTemplateUserRelList(MdmMsgTemplateUserRel mdmMsgTemplateUserRel);

    /**
     * 消息模板绑定用户
     *
     * @param mdmMsgTemplateUserRel
     */
    public int bindUsers(MdmMsgTemplateUserRel mdmMsgTemplateUserRel);

    /**
     * 批量获取模板对应用户数据
     *
     * @param templateCodes
     * @return
     */
    Map<String, String> batchGetAssociatedUsers(List<String> templateCodes);

    /**
     * 新增消息模板关联用户
     *
     * @param mdmMsgTemplateUserRel 消息模板关联用户
     * @return 结果
     */
    @Transactional
    public int insertMdmMsgTemplateUserRel(MdmMsgTemplateUserRel mdmMsgTemplateUserRel);

    /**
     * 修改消息模板关联用户
     *
     * @param mdmMsgTemplateUserRel 消息模板关联用户
     * @return 结果
     */
    @Transactional
    public int updateMdmMsgTemplateUserRel(MdmMsgTemplateUserRel mdmMsgTemplateUserRel);

    /**
     * 批量删除消息模板关联用户
     *
     * @param ids 需要删除的消息模板关联用户主键集合
     * @return 结果
     */

    @Transactional
    public int deleteMdmMsgTemplateUserRelByIds(Long[] ids);

    /**
     * 批量删除消息模板关联用户
     *
     * @param ids 需要删除的消息模板关联用户主键集合
     * @return 结果
     */

    @Transactional
    public int deleteMdmMsgTemplateUserRelByIds(List<Long> ids);

    /**
     * 删除消息模板关联用户信息
     *
     * @param id 消息模板关联用户主键
     * @return 结果
     */
    @Transactional
    public int deleteMdmMsgTemplateUserRelById(Long id);

    /**
     * 校验消息模板关联用户唯一性
     */
    public String checkMdmMsgTemplateUserRelUnique(MdmMsgTemplateUserRel mdmMsgTemplateUserRel);
}
