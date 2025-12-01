package com.zlt.aps.cd15.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.HalfCdImportBak;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IHalfCdImportBakService.java
 * 描    述：IHalfCdImportBakService裁断线下计划导入导出后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-29
 */
public interface IHalfCdImportBakService extends IService<HalfCdImportBak> {

    @Transactional
    AjaxResult importData(List<HalfCdImportBak> list);

    List<HalfCdImportBak> exportDataToList(List<HalfCdImportBak> list, List<HalfCdImportBak> nextDayList, Date scheduleDate);

}
