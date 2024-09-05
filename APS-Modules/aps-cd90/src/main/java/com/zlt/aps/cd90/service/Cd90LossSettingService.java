package com.zlt.aps.cd90.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.dto.Cd90LossSettingDto;
import com.zlt.aps.cd90.entity.Cd90LossSetting;

import java.util.List;

/**
 * 90度裁断损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface Cd90LossSettingService extends IService<Cd90LossSetting> {
    /**
     * 查询90度裁断损耗率设定
     *
     * @param id 90度裁断损耗率设定ID
     * @return 90度裁断损耗率设定
     */
    public Cd90LossSettingDto selectCd90LossSettingById(Long id);

    /**
     * 查询90度裁断损耗率设定列表
     *
     * @param cd90LossSetting 90度裁断损耗率设定
     * @return 90度裁断损耗率设定集合
     */
    public List<Cd90LossSettingDto> selectCd90LossSettingList(Cd90LossSetting cd90LossSetting);

    /**
     * 新增90度裁断损耗率设定
     *
     * @param cd90LossSetting 90度裁断损耗率设定
     * @return 结果
     */
    public int insertCd90LossSetting(Cd90LossSetting cd90LossSetting);

    /**
     * 修改90度裁断损耗率设定
     *
     * @param cd90LossSetting 90度裁断损耗率设定
     * @return 结果
     */
    public int updateCd90LossSetting(Cd90LossSetting cd90LossSetting);

    /**
     * 批量删除90度裁断损耗率设定
     *
     * @param ids 需要删除的90度裁断损耗率设定ID
     * @return 结果
     */
    public int deleteCd90LossSettingByIds(Long[] ids);

    /**
     * 删除90度裁断损耗率设定信息
     *
     * @param id 90度裁断损耗率设定ID
     * @return 结果
     */
    public int deleteCd90LossSettingById(Long id);

    /**
     * 校验90度裁断损耗率设定唯一性
     */
    public String checkCd90LossSettingUnique(Cd90LossSetting cd90LossSetting);

    /**
     * 导入数据
     */
    AjaxResult importData(List<Cd90LossSettingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
