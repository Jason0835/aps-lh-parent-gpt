package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.domain.CxEngineMonthPlanSurplus;
import com.zlt.aps.cx.engine.mapper.CxEngineMonthPlanSurplusMapper;
import com.zlt.aps.cx.engine.service.CxEngineMonthPlanSurplusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
  *  成型工序月度汇总表逻辑层实现业务
  * @ClassName CxEngineMonthPlanSurplusServiceImpl
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/23 15:39
  * @Version 1.0
**/
@Service("cxEngineMonthPlanSurplusService")
@Slf4j
public class CxEngineMonthPlanSurplusServiceImpl implements CxEngineMonthPlanSurplusService {

    @Autowired
    private CxEngineMonthPlanSurplusMapper cxEngineMonthPlanSurplusMapper;

    private Map<String,String> cxParamsMap;

    @Autowired
    private CommonCacheService commonCacheService;

    /**
     *  加载成型工序汇总表已收尾数据集合
     * @param monthPlanApsVersion 月度计划APS版本号
     * @return
     */
    @Override
    public Map<String,CxEngineMonthPlanSurplus> listCxMonthPlanSurplusByMonthPlanApsVersion(String monthPlanApsVersion) {
        Map<String,CxEngineMonthPlanSurplus> closeOutMap=null;
        CxEngineMonthPlanSurplus condition=new CxEngineMonthPlanSurplus();
        condition.setMonthPlanApsVersion(monthPlanApsVersion);
        List<CxEngineMonthPlanSurplus> cxEngineMonthPlanSurplusList=cxEngineMonthPlanSurplusMapper.selectCxMonthPlanSurplusList(condition);
        if(StringUtils.isNotEmpty(cxEngineMonthPlanSurplusList)){
            closeOutMap=cxEngineMonthPlanSurplusList.stream().collect(Collectors.toMap(CxEngineMonthPlanSurplus::getSapCode,cxEngineMonthPlanSurplus -> cxEngineMonthPlanSurplus));
        }else{
            log.debug("月度计划版本号：{}，listCxMonthPlanSurplusByMonthPlanApsVersion》未找到相应的月度汇总信息",monthPlanApsVersion);
        }
        return closeOutMap;
    }

    /**
     * 插入成型外胎汇总表数据
     * @param cxEngineMonthPlanSurplus
     * @return
     */
    @Override
    public int insertCxMonthPlanSurplus(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus) {
        return cxEngineMonthPlanSurplusMapper.insertCxMonthPlanSurplus(cxEngineMonthPlanSurplus);
    }

    @Override
    public List<CxEngineMonthPlanSurplus> listCxEngineMonthPlanSurplus(CxEngineMonthPlanSurplus condition) {
        return cxEngineMonthPlanSurplusMapper.selectCxMonthPlanSurplusList(condition);
    }

    @Override
    public int updateCxMonthPlanSurplus(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus) {
        return cxEngineMonthPlanSurplusMapper.updateCxMonthPlanSurplus(cxEngineMonthPlanSurplus);
    }

    /**
     * 自动排程删除当天外胎汇总插单数据
     * @param cxEngineMonthPlanSurplus
     * @return
     */
    @Override
    public int deleteMonthPlanSurplusByDataSource(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus) {
        return cxEngineMonthPlanSurplusMapper.deleteMonthPlanSurplusByDataSource(cxEngineMonthPlanSurplus);
    }

    /**
     *  插单规格进行调量修正量，外胎汇总表计划调整量，月度剩余量进行更新
     * @param cxEngineMonthPlanSurplus
     * @return
     */
    @Override
    public int updateMonthPlanSurplusBySapCodeVersion(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus) {
        return cxEngineMonthPlanSurplusMapper.updateMonthPlanSurplusBySapCodeVersion(cxEngineMonthPlanSurplus);
    }
}
