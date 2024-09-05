package com.zlt.aps.cd90.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.dto.Cd90LossSettingDto;
import com.zlt.aps.cd90.entity.Cd90LossSetting;

import java.util.List;

/**
 * 90度裁断损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface Cd90LossSettingMapper extends BaseMapper<Cd90LossSetting> {
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
     * 删除90度裁断损耗率设定
     *
     * @param id 90度裁断损耗率设定ID
     * @return 结果
     */
    public int deleteCd90LossSettingById(Long id);

    /**
     * 批量删除90度裁断损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCd90LossSettingByIds(Long[] ids);

    /**
     * 校验90度裁断损耗率设定记录唯一性
     *
     * @param cd90LossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkCd90LossSettingUnique(Cd90LossSetting cd90LossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<Cd90LossSetting> list);

    void deleteAll();
}
