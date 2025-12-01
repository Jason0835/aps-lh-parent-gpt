package com.zlt.mix.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanReceiveQtyDto;
import com.zlt.mix.schedule.api.domain.dto.MaterialScheduleResultStatisticsDto;
import com.zlt.mix.schedule.api.domain.dto.MaterialSpanReceiveQtyDto;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.setting.api.domain.entity.AccessoriesMachine;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 硫磺辅料日计划排程Mapper接口
 *
 * @author chen
 * @date 2022-05-24
 */
public interface MaterialScheduleResultMapper extends BaseMapper<MaterialScheduleResult> {

    /**
     * 查询硫磺辅料日计划排程列表
     *
     * @param materialScheduleResult 硫磺辅料日计划排程
     * @return 硫磺辅料日计划排程集合
     */
    List<MaterialScheduleResult> selectMaterialScheduleResultList(MaterialScheduleResult materialScheduleResult);

    /**
     * 根据id查询排程结果信息
     * @param id id
     * @return 查询到的记录
     */
    MaterialScheduleResult getById(Long id);

    /**
     * 批量删除硫磺辅料日计划排程
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteMaterialScheduleResultByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listMaterialScheduleResultNotUnique(@Param("importList") List<MaterialScheduleResult> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertMaterialScheduleResultInfo(@Param("list") List<MaterialScheduleResult> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<MaterialScheduleResult> list);

    /**
     * 根据ids更改发布状态
     */
    void batchUpdateReleaseStatus(@Param("array") Long[] ids, @Param("status") String status);

    /**
     * 根据集合查询数据库是否唯一
     * @return 重复的记录
     */
    List<MaterialScheduleResult> selectListUnique(@Param("list") Collection<MaterialScheduleResult> list);

    /**
     * 根据参数查询机台信息
     */
    List<LhflMachine> getMachineInfo(LhflMachine param);

    /**
     * 根据id更新配方版本号、配方阶段、配方类型，发布状态
     * @param materialScheduleResult id、配方版本号、配方阶段、配方类型
     */
    void changeRecipe(MaterialScheduleResult materialScheduleResult);

    /**
     * 根据ids查询发布状态是否有不是【未发布】的记录
     * @param ids ids
     * @return 不是未发布的记录数
     */
    int isNoReleaseByIds(@Param("array") Long[] ids);

    /**
     * 根据密炼区和胶料名称查询机台信息
     *
     * @param accessoriesMachine 硫磺辅料与机台对应对象
     * @return 硫磺辅料与机台对应对象列表
     */
    List<AccessoriesMachine> getAccessoriesMachineList(AccessoriesMachine accessoriesMachine);

    /**
     * 根据排程日期删除对应排程日期的所有记录
     * @param materialScheduleResult 排程日期
     * @return 影响行数
     */
    int deleteByScheduleDate(MaterialScheduleResult materialScheduleResult);

    /**
     * 获取统计信息
     * @param materialScheduleResult 日期、密炼区、机台编号
     * @return 统计好的信息列表
     */
    List<MaterialScheduleResultStatisticsDto> statistics(MaterialScheduleResult materialScheduleResult);

    /**
     * 根据id更新记录
     * @param materialScheduleResult 要更新的记录
     */
    void updateScheduleResult(MaterialScheduleResult materialScheduleResult);

    /**
     * 根据id集合查询对应记录，修改后理论上不存在idList内元素过多的情况，不做分段查询处理
     * @param idList id集合
     * @return 查询到的记录
     */
    List<MaterialScheduleResult> selectByIds(@Param("idList") List<Long> idList);

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     * @param scheduleResult 参数
     * @return 结果
     */
    MaterialSpanReceiveQtyDto getSumQtyByMachineCode(MaterialScheduleResult scheduleResult);

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    List<MaterialScheduleResult> selectSpanSendNeedFieldByIds(Long[] ids);
}
