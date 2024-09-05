package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.dto.XwyyLossSettingDto;
import com.zlt.aps.xwyy.entity.XwyyLossSetting;

import java.util.List;

/**
 * 纤维压延损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface XwyyLossSettingService extends IService<XwyyLossSetting> {
    /**
     * 查询纤维压延损耗率设定
     *
     * @param id 纤维压延损耗率设定ID
     * @return 纤维压延损耗率设定
     */
    public XwyyLossSettingDto selectXwyyLossSettingById(Long id);

    /**
     * 查询纤维压延损耗率设定列表
     *
     * @param xwyyLossSetting 纤维压延损耗率设定
     * @return 纤维压延损耗率设定集合
     */
    public List<XwyyLossSettingDto> selectXwyyLossSettingList(XwyyLossSetting xwyyLossSetting);

    /**
     * 新增纤维压延损耗率设定
     *
     * @param xwyyLossSetting 纤维压延损耗率设定
     * @return 结果
     */
    public int insertXwyyLossSetting(XwyyLossSetting xwyyLossSetting);

    /**
     * 修改纤维压延损耗率设定
     *
     * @param xwyyLossSetting 纤维压延损耗率设定
     * @return 结果
     */
    public int updateXwyyLossSetting(XwyyLossSetting xwyyLossSetting);

    /**
     * 批量删除纤维压延损耗率设定
     *
     * @param ids 需要删除的纤维压延损耗率设定ID
     * @return 结果
     */
    public int deleteXwyyLossSettingByIds(Long[] ids);

    /**
     * 删除纤维压延损耗率设定信息
     *
     * @param id 纤维压延损耗率设定ID
     * @return 结果
     */
    public int deleteXwyyLossSettingById(Long id);

    /**
     * 校验纤维压延损耗率设定唯一性
     */
    public String checkXwyyLossSettingUnique(XwyyLossSetting xwyyLossSetting);

    /**
     * 导入数据
     */
    AjaxResult importData(List<XwyyLossSettingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
