package com.zlt.aps.cd90.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.dto.Cd90QuotaSettingDto;
import com.zlt.aps.cd90.entity.Cd90QuotaSetting;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 90度裁断定额设定Service接口
 *
 * @author chen
 * @date 2021-06-29
 */
public interface Cd90QuotaSettingService extends IService<Cd90QuotaSetting> {
    /**
     * 查询90度定额设定列表
     *
     * @param quotaSetting 90度定额设定
     * @return 90度定额设定集合
     */
    public List<Cd90QuotaSettingDto> selectQuotaSettingList(Cd90QuotaSetting quotaSetting);

    /**
     * 查询90度定额设定
     *
     * @param id 90度定额设定ID
     * @return 90度定额设定
     */
    public Cd90QuotaSetting selectQuotaSettingById(Long id);

    /**
     * 修改90度定额设定
     *
     * @param quotaSetting 90度定额设定
     */
    @Transactional
    public AjaxResult saveQuotaSetting(Cd90QuotaSetting quotaSetting);

    /**
     * 批量删除90度定额设定
     *
     * @param ids 需要删除的90度定额设定ID
     */
    @Transactional
    public void deleteQuotaSettingByIds(Long[] ids);

    /**
     * 验证定额设定信息唯一性
     */
    public String checkUnique(Cd90QuotaSetting quotaSetting);

    /**
     * 导入数据
     */
    AjaxResult importData(List<Cd90QuotaSettingDto> list, boolean updateSupport, Long importLogId);
}
