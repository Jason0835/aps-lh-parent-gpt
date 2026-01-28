package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.utils.DateUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.mapper.MdmMsgTemplateUserRelMapper;
import com.zlt.aps.maindata.service.IMdmMsgTemplateUserRelService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMsgTemplateUserRel;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.ruoyi.common.datasource.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.collections4.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import com.zlt.common.utils.ImportExcelValidatedUtils;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMsgTemplateUserRelServiceImpl.java
 * 描    述：MdmMsgTemplateUserRelServiceImpl消息模板关联用户业务层处理
 *@author hc
 *@date 2026-01-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hc
 *     修改内容：...
 */
@Slf4j
@Service
public class MdmMsgTemplateUserRelServiceImpl extends ServiceImpl<MdmMsgTemplateUserRelMapper,MdmMsgTemplateUserRel> implements IMdmMsgTemplateUserRelService
{
    @Autowired
    private MdmMsgTemplateUserRelMapper mdmMsgTemplateUserRelMapper;


    /**
     * 查询消息模板关联用户
     * 
     * @param id 消息模板关联用户主键
     * @return 消息模板关联用户
     */
    @Override
    public MdmMsgTemplateUserRel selectMdmMsgTemplateUserRelById(Long id)
    {
        return mdmMsgTemplateUserRelMapper.selectById(id);
    }

    /**
     * 查询消息模板关联用户列表
     * 
     * @param mdmMsgTemplateUserRel 消息模板关联用户
     * @return 消息模板关联用户
     */
    @Override
    public List<MdmMsgTemplateUserRel> selectMdmMsgTemplateUserRelList(MdmMsgTemplateUserRel mdmMsgTemplateUserRel)
    {
        return mdmMsgTemplateUserRelMapper.selectMdmMsgTemplateUserRelList(mdmMsgTemplateUserRel);
    }

    @Override
    public int bindUsers(MdmMsgTemplateUserRel mdmMsgTemplateUserRel) {
        if (StringUtils.isEmpty(mdmMsgTemplateUserRel.getTemplateCode())){
            throw new BusinessException("请先选择一条消息模板！");
        }
        if (StringUtils.isEmpty(mdmMsgTemplateUserRel.getUserName())){
            throw new BusinessException("请至少选择一个关联用户！");
        }

        String templateCode = mdmMsgTemplateUserRel.getTemplateCode();
        // 1、根据模板编号先查询之前模板绑定的用户数据，存在用户数据则删除关联模板的用户数据
        // 直接构建删除条件，删除该模板下所有的关联记录
        // 使用 QueryWrapper 构建删除条件: WHERE template_code = ?
        LambdaQueryWrapper<MdmMsgTemplateUserRel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmMsgTemplateUserRel::getTemplateCode, templateCode);
        this.remove(queryWrapper); // 执行删除操作

        // 2、根据选择的用户1,用户2，根据逗号获取数组，循环数组生成新的关联模板的用户数据
        String[] userNames = mdmMsgTemplateUserRel.getUserName().split(",");

        List<MdmMsgTemplateUserRel> listToInsert = new ArrayList<>();

        // 假设 MdmMsgTemplateUserRel 实体类中有 setId, setTemplateCode, setUserName 等方法
        // 如果有其他必填字段（如创建时间等），建议在这里通过实体类设置或者在数据库层面配置默认值
        for (String userName : userNames) {
            // 去除用户名可能存在的首尾空格
            userName = userName.trim();
            if (StringUtils.isEmpty(userName)) {
                continue;
            }

            MdmMsgTemplateUserRel rel = new MdmMsgTemplateUserRel();
            rel.setTemplateCode(templateCode);
            rel.setUserName(userName);
            listToInsert.add(rel);
        }

        // 批量保存新数据
        if (!listToInsert.isEmpty()) {
            boolean isSuccess = this.saveBatch(listToInsert);
            return isSuccess ? listToInsert.size() : 0;
        }
        return 0;
    }


    /**
     * 新增消息模板关联用户
     * 
     * @param mdmMsgTemplateUserRel 消息模板关联用户
     * @return 结果
     */
    @Override
    public int insertMdmMsgTemplateUserRel(MdmMsgTemplateUserRel mdmMsgTemplateUserRel)
    {
        mdmMsgTemplateUserRel.setBaseVale(null);
        return mdmMsgTemplateUserRelMapper.insert(mdmMsgTemplateUserRel);
    }

    /**
     * 修改消息模板关联用户
     * 
     * @param mdmMsgTemplateUserRel 消息模板关联用户
     * @return 结果
     */
    @Override
    public int updateMdmMsgTemplateUserRel(MdmMsgTemplateUserRel mdmMsgTemplateUserRel)
    {
        mdmMsgTemplateUserRel.setBaseVale(mdmMsgTemplateUserRel.getId());
        return mdmMsgTemplateUserRelMapper.updateById(mdmMsgTemplateUserRel);
    }

    /**
     * 批量删除消息模板关联用户
     * 
     * @param ids 需要删除的消息模板关联用户主键
     * @return 结果
     */
    @Override
    public int deleteMdmMsgTemplateUserRelByIds(Long[] ids)
    {
        return mdmMsgTemplateUserRelMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 批量删除消息模板关联用户
     *
     * @param ids 需要删除的消息模板关联用户主键
     * @return 结果
     */
    @Override
    public int deleteMdmMsgTemplateUserRelByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMdmMsgTemplateUserRelByIds(arrayids);
    }

    /**
     * 删除消息模板关联用户信息
     * 
     * @param id 消息模板关联用户主键
     * @return 结果
     */
    @Override
    public int deleteMdmMsgTemplateUserRelById(Long id)
    {
        return mdmMsgTemplateUserRelMapper.deleteById(id);
    }

    /**
     * 校验消息模板关联用户唯一性
     */
    @Override
    public String checkMdmMsgTemplateUserRelUnique(MdmMsgTemplateUserRel mdmMsgTemplateUserRel) {
        if (mdmMsgTemplateUserRel == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MdmMsgTemplateUserRel> list = mdmMsgTemplateUserRelMapper.selectMdmMsgTemplateUserRelList(mdmMsgTemplateUserRel);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mdmMsgTemplateUserRel.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
