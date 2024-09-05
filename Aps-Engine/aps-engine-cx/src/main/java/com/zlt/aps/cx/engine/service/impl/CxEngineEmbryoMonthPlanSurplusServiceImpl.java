package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.domain.CxEngineEmbryoMonthPlanSurplus;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxEngineEmbryoMonthPlanSurplusMapper;
import com.zlt.aps.cx.engine.service.CxEngineEmbryoMonthPlanSurplusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成型胎胚维度月度计划汇总表
 */
@Service("cxEngineEmbryoMonthPlanSurplusService")
@Slf4j
public class CxEngineEmbryoMonthPlanSurplusServiceImpl implements CxEngineEmbryoMonthPlanSurplusService {

    @Autowired
    private CxEngineEmbryoMonthPlanSurplusMapper cxEngineEmbryoMonthPlanSurplusMapper;

    @Autowired
    private CommonCacheService commonCacheService;

    /**
     *
     * @param cxEngineEmbryoMonthPlanSurplus
     * @return
     */
    @Override
    public List<CxEngineEmbryoMonthPlanSurplus> selectCxEmbryoMonthPlanSurplusList(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus) {
        return cxEngineEmbryoMonthPlanSurplusMapper.selectCxEmbryoMonthPlanSurplusList(cxEngineEmbryoMonthPlanSurplus);
    }

    @Override
    public Map<String, CxEngineEmbryoMonthPlanSurplus> listCxEmbryoMonthPlanSurplusByMonthPlanApsVersion(String monthPlanApsVersion) {
        Map<String,CxEngineEmbryoMonthPlanSurplus> closeOutMap=null;
        CxEngineEmbryoMonthPlanSurplus condition=new CxEngineEmbryoMonthPlanSurplus();
        condition.setMonthPlanApsVersion(monthPlanApsVersion);
        List<CxEngineEmbryoMonthPlanSurplus> cxEngineEmbryoMonthPlanSurplusList=selectCxEmbryoMonthPlanSurplusList(condition);
        if(StringUtils.isEmpty(cxEngineEmbryoMonthPlanSurplusList)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.embryo.sulplus.empty.error"));
        }
        closeOutMap=new HashMap<>(cxEngineEmbryoMonthPlanSurplusList.size());
        String embryoCode="";
        //提示收尾数量工序参数设置值
//        Integer closeOutNumber=commonCacheService.getCloseOutTipSetting();
        for(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus:cxEngineEmbryoMonthPlanSurplusList){
            cxEngineEmbryoMonthPlanSurplus.setMarkCloseOutTip(false);//不标识收尾提示
            cxEngineEmbryoMonthPlanSurplus.setIsCloseOut(false);//尚未收尾但需要标识收尾标识
            if(cxEngineEmbryoMonthPlanSurplus.getMonthRemainQty()<=0L){
                cxEngineEmbryoMonthPlanSurplus.setIsCloseOut(true);//已收尾
            }/*else if(cxEngineEmbryoMonthPlanSurplus.getMonthRemainQty()<=closeOutNumber){
                cxEngineEmbryoMonthPlanSurplus.setMarkCloseOutTip(true);//标识收尾提示
            }*/
            embryoCode=cxEngineEmbryoMonthPlanSurplus.getEmbryoCode();
            closeOutMap.put(embryoCode,cxEngineEmbryoMonthPlanSurplus);
        }
        return closeOutMap;
    }

    @Override
    public int insertCxEmbryoMonthPlanSurplus(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus) {
        return cxEngineEmbryoMonthPlanSurplusMapper.insertCxEmbryoMonthPlanSurplus(cxEngineEmbryoMonthPlanSurplus);
    }

    @Override
    public int updateCxEmbryoMonthPlanSurplus(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus) {
        return cxEngineEmbryoMonthPlanSurplusMapper.updateCxEmbryoMonthPlanSurplus(cxEngineEmbryoMonthPlanSurplus);
    }

    /**
     * 自动排程删除胎胚当天插单数据
     * @param cxEngineEmbryoMonthPlanSurplus
     * @return
     */
    @Override
    public int deleteEmbryoMonthPlanSurplusByDataSource(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus) {
        return cxEngineEmbryoMonthPlanSurplusMapper.deleteEmbryoMonthPlanSurplusByDataSource(cxEngineEmbryoMonthPlanSurplus);
    }

    /**
     * 插单调整更新计划调整量和月度剩余量
     * @param cxEngineEmbryoMonthPlanSurplus
     * @return
     */
    @Override
    public int updateMonthPlanSurplusByEmbryoCodeVersion(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus) {
        return cxEngineEmbryoMonthPlanSurplusMapper.updateMonthPlanSurplusByEmbryoCodeVersion(cxEngineEmbryoMonthPlanSurplus);
    }
}
