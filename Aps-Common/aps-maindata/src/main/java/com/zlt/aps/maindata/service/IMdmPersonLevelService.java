package com.zlt.aps.maindata.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.mp.api.domain.entity.MdmPersonLevel;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmPersonLevelService.java
 * 描    述：IMdmPersonLevelService成型机人员档配置后端接口
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-20
 */
public interface IMdmPersonLevelService extends IService<MdmPersonLevel> {


    /**
     * 查询成型机人员档配置
     *
     * @param id 成型机人员档配置主键
     * @return 成型机人员档配置
     */
    public MdmPersonLevel selectMdmPersonLevelById(Long id);

    /**
     * 查询成型机人员档配置列表
     *
     * @param mdmPersonLevel 成型机人员档配置
     * @return 成型机人员档配置集合
     */
    public List<MdmPersonLevel> selectMdmPersonLevelList(MdmPersonLevel mdmPersonLevel);

    /**
     * 校验唯一性
     *
     * @param mdmPersonLevel
     * @return
     */
    public String checkUnique(MdmPersonLevel mdmPersonLevel);
}
