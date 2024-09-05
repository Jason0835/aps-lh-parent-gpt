package com.zlt.aps.cx.service;

import java.util.Date;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.aps.cx.api.domain.dto.CxCheckConstructionResultDto;
import com.zlt.aps.cx.api.domain.entity.CxCheckConstruction;

/**
 * 施工信息检测Service接口
 * 
 * @author Gim
 * @date 2022-03-09
 */
public interface CxCheckConstructionService
{

    /**
     * 查询施工信息检测列表
     * 
     * @param cxCheckConstruction 施工信息检测
     * @return 施工信息检测集合
     */
    public List<CxCheckConstruction> selectCxCheckConstructionList(CxCheckConstruction cxCheckConstruction);

    /**
     * 新增施工信息检测
     * 
     * @param cxCheckConstruction 施工信息检测
     * @return 结果
     */
    @Transactional
    public int insertCxCheckConstruction(CxCheckConstruction cxCheckConstruction);
    
    /**
     * 检查月度计划的施工信息
     * @param planMonth	计划月份
     * @return
     */
    List<CxCheckConstructionResultDto> checkMonthPlanConstructionList(Date planMonth);
    
    /**
     * 检查施工信息完整性
     * @param embryoCode	胎胚号
     * @param bomVersion	施工版本
     * @return	检查结果
     */
    String checkConstruction(String embryoCode, String bomVersion);
}
