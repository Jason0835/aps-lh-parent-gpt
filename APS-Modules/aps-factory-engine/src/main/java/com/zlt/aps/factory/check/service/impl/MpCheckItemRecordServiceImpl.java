package com.zlt.aps.factory.check.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.utils.JsonUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.check.mapper.MpCheckItemRecordMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpCheckItemRecord;
import com.zlt.aps.factory.check.service.IMpCheckItemRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.collections4.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpCheckItemRecordServiceImpl.java
 * 描    述：MpCheckItemRecordServiceImplS2-1202 检测项记录业务层处理
 *@author hsc
 *@date 2026-01-29
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hsc
 *     修改内容：...
 */
@Slf4j
@Service
public class MpCheckItemRecordServiceImpl extends ServiceImpl<MpCheckItemRecordMapper, MpCheckItemRecord> implements IMpCheckItemRecordService
{
    @Autowired
    private MpCheckItemRecordMapper mpCheckItemRecordMapper;


    /**
     * 查询S2-1202 检测项记录
     * 
     * @param id S2-1202 检测项记录主键
     * @return S2-1202 检测项记录
     */
    @Override
    public MpCheckItemRecord selectMpCheckItemRecordById(Long id)
    {
        return mpCheckItemRecordMapper.selectById(id);
    }

    /**
     * 查询S2-1202 检测项记录列表
     * 
     * @param mpCheckItemRecord S2-1202 检测项记录
     * @return S2-1202 检测项记录
     */
    @Override
    public List<MpCheckItemRecord> selectMpCheckItemRecordList(MpCheckItemRecord mpCheckItemRecord)
    {
        List<MpCheckItemRecord> mpCheckItemRecordList = mpCheckItemRecordMapper.selectMpCheckItemRecordList(mpCheckItemRecord);
        //获取当前语言包
        Locale language = SecurityUtils.getUserLang();
        JsonUtils.parseJsonRemarkListWithLineBreak(mpCheckItemRecordList, language.toString(), "checkContent");

        return mpCheckItemRecordList;
    }

    /**
     * 新增S2-1202 检测项记录
     * 
     * @param mpCheckItemRecord S2-1202 检测项记录
     * @return 结果
     */
    @Override
    public int insertMpCheckItemRecord(MpCheckItemRecord mpCheckItemRecord)
    {
        mpCheckItemRecord.setBaseVale(null);
        return mpCheckItemRecordMapper.insert(mpCheckItemRecord);
    }

    /**
     * 修改S2-1202 检测项记录
     * 
     * @param mpCheckItemRecord S2-1202 检测项记录
     * @return 结果
     */
    @Override
    public int updateMpCheckItemRecord(MpCheckItemRecord mpCheckItemRecord)
    {
        mpCheckItemRecord.setBaseVale(mpCheckItemRecord.getId());
        return mpCheckItemRecordMapper.updateById(mpCheckItemRecord);
    }

    /**
     * 批量删除S2-1202 检测项记录
     * 
     * @param ids 需要删除的S2-1202 检测项记录主键
     * @return 结果
     */
    @Override
    public int deleteMpCheckItemRecordByIds(Long[] ids)
    {
        return mpCheckItemRecordMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 批量删除S2-1202 检测项记录
     *
     * @param ids 需要删除的S2-1202 检测项记录主键
     * @return 结果
     */
    @Override
    public int deleteMpCheckItemRecordByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMpCheckItemRecordByIds(arrayids);
    }

    /**
     * 删除S2-1202 检测项记录信息
     * 
     * @param id S2-1202 检测项记录主键
     * @return 结果
     */
    @Override
    public int deleteMpCheckItemRecordById(Long id)
    {
        return mpCheckItemRecordMapper.deleteById(id);
    }


    /**
     * 校验S2-1202 检测项记录唯一性
     */
    @Override
    public String checkMpCheckItemRecordUnique(MpCheckItemRecord mpCheckItemRecord) {
        if (mpCheckItemRecord == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpCheckItemRecord> list = mpCheckItemRecordMapper.selectMpCheckItemRecordList(mpCheckItemRecord);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mpCheckItemRecord.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public void clearInvalidData() {
        LambdaQueryWrapper<MpCheckItemRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpCheckItemRecord::getIsDelete, ApsConstant.DEL_FLAG_DEL);
        mpCheckItemRecordMapper.delete(wrapper);
    }
}
