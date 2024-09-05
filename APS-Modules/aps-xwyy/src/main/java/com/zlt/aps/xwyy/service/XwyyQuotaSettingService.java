package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.dto.XwyyQuotaSettingDto;
import com.zlt.aps.xwyy.entity.XwyyQuotaSetting;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 纤维压延定额设定Service接口
 *
 * @author chen
 * @date 2021-06-29
 */
public interface XwyyQuotaSettingService extends IService<XwyyQuotaSetting> {
    /**
     * 查询纤维压延定额设定列表
     *
     * @param quotaSetting 纤维压延定额设定
     * @return 纤维压延定额设定集合
     */
    public List<XwyyQuotaSettingDto> selectQuotaSettingList(XwyyQuotaSetting quotaSetting);

    /**
     * 查询纤维压延定额设定
     *
     * @param id 纤维压延定额设定ID
     * @return 纤维压延定额设定
     */
    public XwyyQuotaSetting selectQuotaSettingById(Long id);

    /**
     * 修改纤维压延定额设定
     *
     * @param quotaSetting 纤维压延定额设定
     */
    @Transactional
    public AjaxResult saveQuotaSetting(XwyyQuotaSetting quotaSetting);

    /**
     * 批量删除纤维压延定额设定
     *
     * @param ids 需要删除的纤维压延定额设定ID
     */
    @Transactional
    public void deleteQuotaSettingByIds(Long[] ids);

    /**
     * 验证定额设定信息唯一性
     */
    public String checkUnique(XwyyQuotaSetting quotaSetting);

    /**
     * 导入数据
     */
    AjaxResult importData(List<XwyyQuotaSettingDto> list, boolean updateSupport, Long importLogId);

}
