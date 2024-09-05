package com.zlt.aps.cx.engine.mapper;


import com.zlt.aps.cx.engine.domain.CxEngineHolidaySetting;

import java.util.List;

/**
 * 成型节假日设定Mapper接口
 * 
 * @author Joran.zhang
 * @date 2021-06-29
 */
public interface CxEngineHolidaySettingMapper
{
    /**
     * 根据条件查询节假日信息列表
     * @param cxEngineHolidaySetting
     * @return
     */
    public List<CxEngineHolidaySetting> selectCxEngineHolidaySettingList(CxEngineHolidaySetting cxEngineHolidaySetting);


    /**
     * 根据条件获取节假日数量
     * @param cxEngineHolidaySetting
     * @return
     */
    public int countOfHolidaySetting(CxEngineHolidaySetting cxEngineHolidaySetting);
}
