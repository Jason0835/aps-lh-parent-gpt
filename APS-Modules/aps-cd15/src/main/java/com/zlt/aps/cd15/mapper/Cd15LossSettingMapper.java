package com.zlt.aps.cd15.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.dto.Cd15LossSettingDto;
import com.zlt.aps.cd15.entity.Cd15LossSetting;

import java.util.List;

/**
 * 15度裁断损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface Cd15LossSettingMapper extends BaseMapper<Cd15LossSetting> {
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
     * 删除15度裁断损耗率设定
     *
     * @param id 15度裁断损耗率设定ID
     * @return 结果
     */
    public int deleteCd15LossSettingById(Long id);

    /**
     * 批量删除15度裁断损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCd15LossSettingByIds(Long[] ids);

    /**
     * 校验90度裁断损耗率设定记录唯一性
     *
     * @param cd15LossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkCd15LossSettingUnique(Cd15LossSetting cd15LossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<Cd15LossSettingDto> list);

    void deleteAll();
}
