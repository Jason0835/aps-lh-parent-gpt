package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.dto.TqLossSettingDto;
import com.zlt.aps.tq.entity.TqLossSetting;

import java.util.List;

/**
 * 胎圈损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-13
 */
public interface TqLossSettingMapper extends BaseMapper<TqLossSetting> {

    /**
     * 查询胎圈损耗率设定
     *
     * @param id 胎圈损耗率设定ID
     * @return 胎圈损耗率设定
     */
    public TqLossSettingDto selectTqLossSettingById(Long id);

    /**
     * 查询胎圈损耗率设定列表
     *
     * @param tqLossSetting 胎圈损耗率设定
     * @return 胎圈损耗率设定集合
     */
    public List<TqLossSettingDto> selectTqLossSettingList(TqLossSetting tqLossSetting);

    /**
     * 新增胎圈损耗率设定
     *
     * @param tqLossSetting 胎圈损耗率设定
     * @return 结果
     */
    public int insertTqLossSetting(TqLossSetting tqLossSetting);

    /**
     * 修改胎圈损耗率设定
     *
     * @param tqLossSetting 胎圈损耗率设定
     * @return 结果
     */
    public int updateTqLossSetting(TqLossSetting tqLossSetting);

    /**
     * 删除胎圈损耗率设定
     *
     * @param id 胎圈损耗率设定ID
     * @return 结果
     */
    public int deleteTqLossSettingById(Long id);

    /**
     * 批量删除胎圈损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTqLossSettingByIds(Long[] ids);

    /**
     * 校验胎圈损耗率设定记录唯一性
     *
     * @param tqLossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkTqLossSettingUnique(TqLossSetting tqLossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TqLossSettingDto> list);

    void deleteAll();
}
