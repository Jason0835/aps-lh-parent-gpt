package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.tm.api.domain.entity.HalfYcImportBakExportData;
import com.zlt.aps.tm.mapper.HalfYcImportBakExportDataEntityMapper;
import com.zlt.aps.tm.service.IHalfYcImportBakExportDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：HalfYcImportBakServiceImpl.java
 * 描    述：HalfYcImportBakServiceImpl线下计划导入业务层处理
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
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class HalfYcImportBakExportDataServiceImpl extends ServiceImpl<HalfYcImportBakExportDataEntityMapper, HalfYcImportBakExportData> implements IHalfYcImportBakExportDataService {
}
