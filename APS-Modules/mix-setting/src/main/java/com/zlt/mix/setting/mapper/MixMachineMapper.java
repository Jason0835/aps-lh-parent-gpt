package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.List;

/**
 * 密炼机台信息Mapper接口
 * 
 * @author Gim
 * @date 2022-03-22
 */
public interface MixMachineMapper extends BaseMapper<MixMachine> {

    /**
     * 查询密炼机台信息列表
     * 
     * @param mixMachine 密炼机台信息
     * @return 密炼机台信息集合
     */
    List<MixMachine> selectMixMachineList(MixMachine mixMachine);

    /**
     * 批量删除密炼机台信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteMixMachineByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listMixMachineNotUnique(@Param("importList") List<MixMachine> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    List<ImportErrorLog> listMixMachineNotUnique2(@Param("importList") List<MixMachine> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);


    /**
     * 检测是否存在仅有密炼区和机台名称冲突，而机台编号不冲突的记录
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy    创建者
     * @return
     */
    List<ImportErrorLog> listMixMachineNotUnique3(@Param("importList") List<MixMachine> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertMixMachineInfo(@Param("list") List<MixMachine> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<MixMachine> list);

    /**
     * 查询所有机台信息(包含硫磺辅料机台信息)
     * @return 查询到的机台信息
     */
    ArrayList<MixMachine> getAllMachineInfo();
}
