package com.zlt.aps.mp.engine.check.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.monthplan.api.domain.entity.MpCheckItemRecord;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpCheckItemRecordService.java
 * 描    述：IMpCheckItemRecordServiceS2-1202 检测项记录后端接口
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2026-01-29
 */
public interface IMpCheckItemRecordService extends IService<MpCheckItemRecord> {
    /**
     * 查询S2-1202 检测项记录
     *
     * @param id S2-1202 检测项记录主键
     * @return S2-1202 检测项记录
     */
    public MpCheckItemRecord selectMpCheckItemRecordById(Long id);

    /**
     * 查询S2-1202 检测项记录列表
     *
     * @param mpCheckItemRecord S2-1202 检测项记录
     * @return S2-1202 检测项记录集合
     */
    public List<MpCheckItemRecord> selectMpCheckItemRecordList(MpCheckItemRecord mpCheckItemRecord);


    /**
     * 新增S2-1202 检测项记录
     *
     * @param mpCheckItemRecord S2-1202 检测项记录
     * @return 结果
     */
    @Transactional
    public int insertMpCheckItemRecord(MpCheckItemRecord mpCheckItemRecord);

    /**
     * 修改S2-1202 检测项记录
     *
     * @param mpCheckItemRecord S2-1202 检测项记录
     * @return 结果
     */
    @Transactional
    public int updateMpCheckItemRecord(MpCheckItemRecord mpCheckItemRecord);

    /**
     * 批量删除S2-1202 检测项记录
     *
     * @param ids 需要删除的S2-1202 检测项记录主键集合
     * @return 结果
     */

    @Transactional
    public int deleteMpCheckItemRecordByIds(Long[] ids);

    /**
     * 批量删除S2-1202 检测项记录
     *
     * @param ids 需要删除的S2-1202 检测项记录主键集合
     * @return 结果
     */

    @Transactional
    public int deleteMpCheckItemRecordByIds(List<Long> ids);

    /**
     * 删除S2-1202 检测项记录信息
     *
     * @param id S2-1202 检测项记录主键
     * @return 结果
     */
    @Transactional
    public int deleteMpCheckItemRecordById(Long id);

    /**
     * 校验S2-1202 检测项记录唯一性
     */
    public String checkMpCheckItemRecordUnique(MpCheckItemRecord mpCheckItemRecord);

    /**
     * 清理无效数据
     */
    public void clearInvalidData();
}
