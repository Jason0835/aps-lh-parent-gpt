package com.zlt.mix.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.schedule.api.domain.vo.MlImportBak;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMlImportBakService.java
 * 描    述：IMlImportBakService密炼线下计划操作功能后端接口
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
public interface IMlImportBakService extends IService<MlImportBak> {

    /**
     * 线下排程数据导入
     *
     * @param list        要导入的列表数据
     * @param date        排程时间
     * @param mixArea     密炼区
     * @param importLogId 导入日志id
     * @return 结果
     */
    AjaxResult importOfflineData(List<MlImportBak> list, Date date, String mixArea, Long importLogId);
}
