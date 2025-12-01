package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cd15.api.domain.entity.HalfCdImportBakExportData;
import com.zlt.aps.cd15.mapper.HalfCdImportBakExportDataEntityMapper;
import com.zlt.aps.cd15.service.IHalfCdImportBakExportDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：HalfCdImportBakServiceImpl.java
 * 描    述：HalfCdImportBakServiceImpl裁断线下计划导入导出业务层处理
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
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class HalfCdImportBakExportDataServiceImpl extends ServiceImpl<HalfCdImportBakExportDataEntityMapper, HalfCdImportBakExportData> implements IHalfCdImportBakExportDataService {

}
