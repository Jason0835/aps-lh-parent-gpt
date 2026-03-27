package com.zlt.aps.cx.mapper.entity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxHolidaySettingDto;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxHolidaySetting;


import java.util.List;

/**
 * 假日设定Mapper接口
 *
 * @author chen
 * @date 2021-06-30
 */
public interface CxHolidaySettingMapper extends BaseMapper<CxHolidaySetting> {
    /**
     * 查询成型定额设定列表
     *
     * @param dto 成型定额设定
     * @return 成型定额设定集合
     */
    public List<CxHolidaySettingDto> selectCxHolidaySettingList(CxHolidaySetting dto);

    /**
     * 校验记录唯一性
     *
     * @param dto 要校验记录
     * @return 查询到的结果
     */
    public List<CxHolidaySettingDto> checkUnique(CxHolidaySetting dto);

    public List<CxHolidaySettingDto> checkUnique2(CxHolidaySetting dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxHolidaySetting> list);

}
