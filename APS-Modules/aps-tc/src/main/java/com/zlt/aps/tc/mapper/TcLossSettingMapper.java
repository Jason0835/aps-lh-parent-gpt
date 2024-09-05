package com.zlt.aps.tc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tc.api.domain.dto.TcLossSettingDto;
import com.zlt.aps.tc.entity.TcLossSetting;

import java.util.List;

/**
 * 胎侧损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-13
 */
public interface TcLossSettingMapper extends BaseMapper<TcLossSetting> {
    /**
     * 查询胎侧损耗率设定
     *
     * @param id 胎侧损耗率设定ID
     * @return 胎侧损耗率设定
     */
    public TcLossSettingDto selectTcLossSettingById(Long id);

    /**
     * 查询胎侧损耗率设定列表
     *
     * @param tcLossSetting 胎侧损耗率设定
     * @return 胎侧损耗率设定集合
     */
    public List<TcLossSettingDto> selectTcLossSettingList(TcLossSetting tcLossSetting);

    /**
     * 新增胎侧损耗率设定
     *
     * @param tcLossSetting 胎侧损耗率设定
     * @return 结果
     */
    public int insertTcLossSetting(TcLossSetting tcLossSetting);

    /**
     * 修改胎侧损耗率设定
     *
     * @param tcLossSetting 胎侧损耗率设定
     * @return 结果
     */
    public int updateTcLossSetting(TcLossSetting tcLossSetting);

    /**
     * 删除胎侧损耗率设定
     *
     * @param id 胎侧损耗率设定ID
     * @return 结果
     */
    public int deleteTcLossSettingById(Long id);

    /**
     * 批量删除胎侧损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTcLossSettingByIds(Long[] ids);

    /**
     * 校验胎侧损耗率设定记录唯一性
     *
     * @param tcLossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkTcLossSettingUnique(TcLossSetting tcLossSetting);

    /**
     * 合并操作，存在则更新，否则新增
     */
    public void mergeSql(List<TcLossSettingDto> list);

    void deleteAll();
}
