package com.zlt.aps.mdm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmMsgTemplateUserRel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMsgTemplateUserRelMapper.java
 * 描    述：消息模板关联用户Mapper接口
 *@author hc
 *@date 2026-01-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hc
 *     修改内容：...
 */
@Mapper
public interface MdmMsgTemplateUserRelMapper extends BaseMapper<MdmMsgTemplateUserRel>
{

    /**
     * 查询消息模板关联用户列表
     *
     * @param mdmMsgTemplateUserRel 消息模板关联用户
     * @return 消息模板关联用户集合
     */
    public List<MdmMsgTemplateUserRel> selectMdmMsgTemplateUserRelList(MdmMsgTemplateUserRel mdmMsgTemplateUserRel);
}
