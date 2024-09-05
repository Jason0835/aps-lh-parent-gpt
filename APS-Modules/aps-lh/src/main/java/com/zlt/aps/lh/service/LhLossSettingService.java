package com.zlt.aps.lh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.dto.LhLossSettingDto;
import com.zlt.aps.lh.entity.LhLossSetting;

import java.util.List;


/**
 * 硫化损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface LhLossSettingService extends IService<LhLossSetting> {
    /**
     * 查询硫化损耗率设定
     *
     * @param id 硫化损耗率设定ID
     * @return 硫化损耗率设定
     */
    public LhLossSettingDto selectLhLossSettingById(Long id);

    /**
     * 查询硫化损耗率设定列表
     *
     * @param lhLossSetting 硫化损耗率设定
     * @return 硫化损耗率设定集合
     */
    public List<LhLossSettingDto> selectLhLossSettingList(LhLossSetting lhLossSetting);

    /**
     * 新增硫化损耗率设定
     *
     * @param lhLossSetting 硫化损耗率设定
     * @return 结果
     */
    public int insertLhLossSetting(LhLossSetting lhLossSetting);

    /**
     * 修改硫化损耗率设定
     *
     * @param lhLossSetting 硫化损耗率设定
     * @return 结果
     */
    public int updateLhLossSetting(LhLossSetting lhLossSetting);

    /**
     * 批量删除硫化损耗率设定
     *
     * @param ids 需要删除的硫化损耗率设定ID
     * @return 结果
     */
    public int deleteLhLossSettingByIds(Long[] ids);

    /**
     * 删除硫化损耗率设定信息
     *
     * @param id 硫化损耗率设定ID
     * @return 结果
     */
    public int deleteLhLossSettingById(Long id);

    /**
     * 校验硫化损耗率设定唯一性
     */
    public String checkLhLossSettingUnique(LhLossSetting lhLossSetting);

    /**
     * 导入数据
     */
    AjaxResult importData(List<LhLossSettingDto> list, boolean updateSupport, Long importLogId);
}
