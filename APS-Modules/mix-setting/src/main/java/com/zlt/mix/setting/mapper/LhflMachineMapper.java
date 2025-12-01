package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 小料机台信息Mapper接口
 *
 * @author Liam
 * @date 2022-04-18
 */
public interface LhflMachineMapper extends BaseMapper<LhflMachine> {

    /**
     * 查询小料机台信息列表
     *
     * @param lhflMachine 小料机台信息
     * @return 小料机台信息集合
     */
    List<LhflMachine> selectLhflMachineList(LhflMachine lhflMachine);

    /**
     * 批量删除小料机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteLhflMachineByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listLhflMachineNotUnique(@Param("importList") List<LhflMachine> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertLhflMachineInfo(@Param("list") List<LhflMachine> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<LhflMachine> list);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listLhflMachineNotUnique2(@Param("importList") List<LhflMachine> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 检测是否存在仅有密炼区和机台名称冲突，而机台编号不冲突的记录
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listLhflMachineNotUnique3(@Param("importList") List<LhflMachine> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);
}
