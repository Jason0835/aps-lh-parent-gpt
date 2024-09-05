package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;

import java.util.List;
import java.util.Map;

/**
 * 硫化外胎施工信息Service接口
 * 
 * @author Joran.zhang
 * @date 2021-09-04
 */
public interface LhEngineTireConstructionInfoService
{

    /**
     * 查询硫化外胎施工信息列表
     * 
     * @param lhEngineTireConstructionInfo 硫化外胎施工信息
     * @return 硫化外胎施工信息集合
     */
    public List<LhEngineTireConstructionInfo> selectLhTireConstructionInfoList(LhEngineTireConstructionInfo lhEngineTireConstructionInfo);

    /**
     * 根据SAP品号和胎胚代码进行获取硫化时长
     * @param sapCode sap品号
     * @param embryoCode  胎胚代码
     * @return
     */
    Double getLhTireTimeBySapCode(String sapCode,String embryoCode);

    /**
     * 获取硫化施工信息
     * @param sapCode
     * @param embryoCode
     * @return
     */
    LhEngineTireConstructionInfo getLhConstructionInfoByCondition(String sapCode,String embryoCode);

    /**
     * 根据外胎品号获取施工如果没有则从工序参数获取硫化时长
     * @param sapCode
     * @param embryoCode
     * @param sapTireConstructionListMap
     * @return
     */
    Double getSingleTireTimeBySap(String sapCode, String embryoCode, Map<String, List<LhEngineTireConstructionInfo>> sapTireConstructionListMap);
}
