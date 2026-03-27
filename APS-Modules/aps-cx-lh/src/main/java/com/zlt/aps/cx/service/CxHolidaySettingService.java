package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxHolidaySettingDto;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxHolidaySetting;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 假日设定Service接口
 *
 * @author chen
 * @date 2021-06-30
 */
public interface CxHolidaySettingService extends IService<CxHolidaySetting> {
    /**
     * 查询成型假日设定列表
     *
     * @param setting 成型假日设定
     * @return 成型假日设定集合
     */
    public List<CxHolidaySettingDto> selectCxHolidaySettingList(CxHolidaySetting setting);

    /**
     * 查询成型假日设定
     *
     * @param id 成型假日设定ID
     * @return 成型假日设定
     */
    public CxHolidaySetting selectCxHolidaySettingById(Long id);

    /**
     * 修改成型假日设定
     *
     * @param dto 成型假日设定
     */
    @Transactional
    public void saveCxHolidaySetting(CxHolidaySettingDto dto);

    /**
     * 批量删除成型假日设定
     *
     * @param ids 需要删除的成型假日设定ID
     */
    @Transactional
    public void deleteCxHolidaySettingByIds(Long[] ids);

    /**
     * 校验记录唯一性
     *
     * @param setting 要校验记录
     * @return 查询到的结果
     */
    public List<CxHolidaySettingDto> checkUnique(CxHolidaySetting setting);

    /**
     * 导入数据
     */
    AjaxResult importData(List<CxHolidaySettingDto> list, boolean updateSupport, Long importLogId);
}
