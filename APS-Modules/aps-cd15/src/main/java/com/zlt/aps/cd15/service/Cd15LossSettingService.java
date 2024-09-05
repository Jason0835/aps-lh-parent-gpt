package com.zlt.aps.cd15.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.dto.Cd15LossSettingDto;
import com.zlt.aps.cd15.entity.Cd15LossSetting;

import java.util.List;

/**
 * 15度裁断损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface Cd15LossSettingService extends IService<Cd15LossSetting> {
    /**
     * 查询15度裁断损耗率设定
     *
     * @param id 15度裁断损耗率设定ID
     * @return 15度裁断损耗率设定
     */
    public Cd15LossSettingDto selectCd15LossSettingById(Long id);

    /**
     * 查询15度裁断损耗率设定列表
     *
     * @param cd15LossSetting 15度裁断损耗率设定
     * @return 15度裁断损耗率设定集合
     */
    public List<Cd15LossSettingDto> selectCd15LossSettingList(Cd15LossSetting cd15LossSetting);

    /**
     * 新增15度裁断损耗率设定
     *
     * @param cd15LossSetting 15度裁断损耗率设定
     * @return 结果
     */
    public int insertCd15LossSetting(Cd15LossSetting cd15LossSetting);

    /**
     * 修改15度裁断损耗率设定
     *
     * @param cd15LossSetting 15度裁断损耗率设定
     * @return 结果
     */
    public int updateCd15LossSetting(Cd15LossSetting cd15LossSetting);

    /**
     * 批量删除15度裁断损耗率设定
     *
     * @param ids 需要删除的15度裁断损耗率设定ID
     * @return 结果
     */
    public int deleteCd15LossSettingByIds(Long[] ids);

    /**
     * 删除15度裁断损耗率设定信息
     *
     * @param id 15度裁断损耗率设定ID
     * @return 结果
     */
    public int deleteCd15LossSettingById(Long id);

    /**
     * 校验15度裁断损耗率设定唯一性
     */
    public String checkCd15LossSettingUnique(Cd15LossSetting cd15LossSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<Cd15LossSettingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
