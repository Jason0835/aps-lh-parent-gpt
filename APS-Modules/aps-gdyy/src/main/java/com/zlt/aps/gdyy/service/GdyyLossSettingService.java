package com.zlt.aps.gdyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.dto.GdyyLossSettingDto;
import com.zlt.aps.gdyy.entity.GdyyLossSetting;

import java.util.List;

/**
 * 钢带压延损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface GdyyLossSettingService extends IService<GdyyLossSetting> {
    /**
     * 查询钢带压延损耗率设定
     *
     * @param id 钢带压延损耗率设定ID
     * @return 钢带压延损耗率设定
     */
    public GdyyLossSettingDto selectGdyyLossSettingById(Long id);

    /**
     * 查询钢带压延损耗率设定列表
     *
     * @param gdyyLossSetting 钢带压延损耗率设定
     * @return 钢带压延损耗率设定集合
     */
    public List<GdyyLossSettingDto> selectGdyyLossSettingList(GdyyLossSetting gdyyLossSetting);

    /**
     * 新增钢带压延损耗率设定
     *
     * @param gdyyLossSetting 钢带压延损耗率设定
     * @return 结果
     */
    public int insertGdyyLossSetting(GdyyLossSetting gdyyLossSetting);

    /**
     * 修改钢带压延损耗率设定
     *
     * @param gdyyLossSetting 钢带压延损耗率设定
     * @return 结果
     */
    public int updateGdyyLossSetting(GdyyLossSetting gdyyLossSetting);

    /**
     * 批量删除钢带压延损耗率设定
     *
     * @param ids 需要删除的钢带压延损耗率设定ID
     * @return 结果
     */
    public int deleteGdyyLossSettingByIds(Long[] ids);

    /**
     * 删除钢带压延损耗率设定信息
     *
     * @param id 钢带压延损耗率设定ID
     * @return 结果
     */
    public int deleteGdyyLossSettingById(Long id);

    /**
     * 校验钢带压延损耗率设定唯一性
     */
    public String checkGdyyLossSettingUnique(GdyyLossSetting gdyyLossSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GdyyLossSettingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
