package com.zlt.aps.tq.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 胎圈机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface TqMachineInfoMapper extends BaseMapper<TqMachineInfo> {

    /**
     * 查询胎圈机台信息列表
     *
     * @param machineInfo 胎圈机台信息
     * @return 胎圈机台信息集合
     */
    public List<TqMachineInfo> selectMachineInfoList(TqMachineInfo machineInfo);

    /**
     * 校验胎圈机台唯一性
     */
    public List<TqMachineInfo> checkMachineCodeUnique(TqMachineInfo machineInfo);
    public List<TqMachineInfo> checkMachineNameUnique(TqMachineInfo machineInfo);

    /**
     * 根据条件查询机台信息
     *
     * @param machineInfo 查询条件
     * @return 结果
     */
    List<TqMachineInfo> listMachineInfo(TqMachineInfo machineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TqMachineInfo> list);

}
