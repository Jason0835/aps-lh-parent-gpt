package com.zlt.mix.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.schedule.api.domain.vo.MlImportBak;
import com.zlt.mix.schedule.mapper.MlImportBakEntityMapper;
import com.zlt.mix.schedule.service.IMlImportBakService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MlImportBakServiceImpl.java
 * 描    述：MlImportBakServiceImpl密炼线下计划操作功能业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-05
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MlImportBakServiceImpl extends ServiceImpl<MlImportBakEntityMapper, MlImportBak> implements IMlImportBakService {

    @Autowired
    private MlImportBakEntityMapper mlImportBakEntityMapper;

    /**
     * 线下排程数据导入
     *
     * @param list        要导入的列表数据
     * @param date        排程时间
     * @param mixArea     密炼区
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Override
    public AjaxResult importOfflineData(List<MlImportBak> list, Date date, String mixArea, Long importLogId) {
        LambdaUpdateWrapper<MlImportBak> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(MlImportBak::getRq, date);
        mlImportBakEntityMapper.delete(queryWrapper);
        for (MlImportBak mlImportBak : list) {
            mlImportBakEntityMapper.insert(mlImportBak);
        }
        mlImportBakEntityMapper.importMlData(date);
        return AjaxResult.success();
    }
}
