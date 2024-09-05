package com.zlt.aps.gdyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.dto.GdyyQuotaSettingDto;
import com.zlt.aps.gdyy.entity.GdyyQuotaSetting;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 钢带压延定额设定Service接口
 *
 * @author chen
 * @date 2021-06-30
 */
public interface GdyyQuotaSettingService extends IService<GdyyQuotaSetting> {

    /**
     * 查询钢带压延定额设定列表
     *
     * @param quotaSetting 钢带压延定额设定
     * @return 钢带压延定额设定集合
     */
    public List<GdyyQuotaSettingDto> selectQuotaSettingList(GdyyQuotaSetting quotaSetting);

    /**
     * 查询钢带压延定额设定
     *
     * @param id 钢带压延定额设定ID
     * @return 钢带压延定额设定
     */
    public GdyyQuotaSetting selectQuotaSettingById(Long id);

    /**
     * 修改钢带压延定额设定
     *
     * @param quotaSetting 钢带压延定额设定
     */
    @Transactional
    public AjaxResult saveQuotaSetting(GdyyQuotaSetting quotaSetting);

    /**
     * 批量删除钢带压延定额设定
     *
     * @param ids 需要删除的钢带压延定额设定ID
     */
    @Transactional
    public void deleteQuotaSettingByIds(Long[] ids);

    /**
     * 验证定额设定信息唯一性
     */
    public String checkUnique(GdyyQuotaSetting quotaSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GdyyQuotaSettingDto> list, boolean updateSupport, Long importLogId);
}
