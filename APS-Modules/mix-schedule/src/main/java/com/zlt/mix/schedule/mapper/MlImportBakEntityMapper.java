package com.zlt.mix.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.schedule.api.domain.vo.MlImportBak;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MlImportBakMapper.java
 * 描    述：密炼线下计划操作功能Mapper接口
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
@Mapper
public interface MlImportBakEntityMapper extends BaseMapper<MlImportBak> {

    /**
     * 线下计划数据导入
     *
     * @param scheduleDate 排程日期
     */
    void importMlData(@Param("scheduleDate") Date scheduleDate);
}
