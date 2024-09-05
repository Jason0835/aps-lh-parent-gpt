package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.dto.CxQuotaSettingDto;
import com.zlt.aps.cx.entity.CxQuotaSetting;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型定额设定Service接口
 *
 * @author chen
 * @date 2021-06-16
 */
public interface CxQuotaSettingService extends IService<CxQuotaSetting> {
    /**
     * 查询成型定额设定列表
     *
     * @param quotaSetting 成型定额设定
     * @return 成型定额设定集合
     */
    public List<CxQuotaSettingDto> selectCxQuotaSettingList(CxQuotaSetting quotaSetting);

    /**
     * 查询成型定额设定
     *
     * @param id 成型定额设定ID
     * @return 成型定额设定
     */
    public CxQuotaSetting selectCxQuotaSettingById(Long id);

    /**
     * 修改成型定额设定
     *
     * @param quotaSetting 成型定额设定
     */
    @Transactional
    public void saveCxQuotaSetting(CxQuotaSetting quotaSetting);

    /**
     * 批量删除成型定额设定
     *
     * @param ids 需要删除的成型定额设定ID
     */
    @Transactional
    public void deleteCxQuotaSettingByIds(Long[] ids);

    /**
     * 导入数据
     */
    AjaxResult importData(List<CxQuotaSettingDto> list, boolean updateSupport, Long importLogId);
}
