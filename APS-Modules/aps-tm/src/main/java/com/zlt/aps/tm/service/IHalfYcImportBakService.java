package com.zlt.aps.tm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.entity.HalfYcImportBak;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IHalfYcImportBakService.java
 * 描    述：IHalfYcImportBakService线下计划导入后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-26
 */
public interface IHalfYcImportBakService extends IService<HalfYcImportBak> {

    AjaxResult importData(List<HalfYcImportBak> list);

    List<HalfYcImportBak> exportDataToList(List<HalfYcImportBak> list, List<HalfYcImportBak> nextDayList, Date scheduleDate);
}
