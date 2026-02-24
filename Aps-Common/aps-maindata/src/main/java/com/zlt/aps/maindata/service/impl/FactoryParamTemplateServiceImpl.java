package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.maindata.mapper.FactoryParamTemplateMapper;
import com.zlt.aps.maindata.service.IFactoryParamTemplateService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParamTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryParamTemplateServiceImpl.java
 * 描    述：FactoryParamTemplateServiceImpl系统参数设置模板业务层处理
 *@author zlt
 *@date 2025-02-26
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class FactoryParamTemplateServiceImpl extends ServiceImpl<FactoryParamTemplateMapper, FactoryParamTemplate> implements IFactoryParamTemplateService {


}
