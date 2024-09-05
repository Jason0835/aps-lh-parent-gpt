package com.zlt.aps.cd15.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.dto.Cd15QuotaSettingDto;
import com.zlt.aps.cd15.entity.Cd15QuotaSetting;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 15度裁断定额设定Service接口
 *
 * @author chen
 * @date 2021-06-28
 */
public interface Cd15QuotaSettingService extends IService<Cd15QuotaSetting> {
    /**
     * 查询15度定额设定列表
     *
     * @param quotaSetting 15度定额设定
     * @return 15度定额设定集合
     */
    public List<Cd15QuotaSettingDto> selectQuotaSettingList(Cd15QuotaSetting quotaSetting);

    /**
     * 查询15度定额设定
     *
     * @param id 15度定额设定ID
     * @return 15度定额设定
     */
    public Cd15QuotaSetting selectQuotaSettingById(Long id);

    /**
     * 修改15度定额设定
     *
     * @param quotaSetting 15度定额设定
     */
    @Transactional
    public AjaxResult saveQuotaSetting(Cd15QuotaSetting quotaSetting);

    /**
     * 批量删除15度定额设定
     *
     * @param ids 需要删除的15度定额设定ID
     */
    @Transactional
    public void deleteQuotaSettingByIds(Long[] ids);

    /**
     * 验证定额设定信息唯一性
     */
    public String checkUnique(Cd15QuotaSetting quotaSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<Cd15QuotaSettingDto> list, boolean updateSupport, Long importLogId);
}
