package com.zlt.mix.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.common.core.vo.ScheduleSummaryVo;
import com.zlt.mix.schedule.api.domain.dto.GlueScheduleResultStatisticsDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanReceiveQtyDto;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.setting.api.domain.entity.FormulaMachine;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 终炼/母炼日计划排程Mapper接口
 *
 * @author chen
 * @date 2022-05-16
 */
public interface GlueScheduleResultMapper extends BaseMapper<GlueScheduleResult> {

    /**
     * 查询终炼/母炼日计划排程列表
     *
     * @param glueScheduleResult 终炼/母炼日计划排程
     * @return 终炼/母炼日计划排程集合
     */
    List<GlueScheduleResult> selectGlueScheduleResultList(GlueScheduleResult glueScheduleResult);

    /**
     * 根据id查询排程结果信息
     * @param id id
     * @return 查询到的记录
     */
    GlueScheduleResult getById(Long id);

    /**
     * 批量删除终炼/母炼日计划排程
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteGlueScheduleResultByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listGlueScheduleResultNotUnique(@Param("importList") List<GlueScheduleResult> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertGlueScheduleResultInfo(@Param("list") List<GlueScheduleResult> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<GlueScheduleResult> list);

    /**
     * 根据ids更改发布状态
     */
    void batchUpdateReleaseStatus(@Param("array") Long[] ids, @Param("status") String status);

    /**
     * 根据集合查询数据库是否唯一
     * @return 重复的记录
     */
    List<GlueScheduleResult> selectListUnique(@Param("list") Collection<GlueScheduleResult> list);

    /**
     * 根据参数查询机台信息
     */
    List<MixMachine> getMachineInfo(MixMachine param);

    /**
     * 根据参数查询配方信息
     * @param param 参数
     * @return 查询到的结果信息
     */
    List<RecipeType> getRecipeTypeInfo(RecipeType param);

    /**
     * 根据id更新配方版本号、配方阶段、配方类型，发布状态
     * @param glueScheduleResult id、配方版本号、配方阶段、配方类型
     */
    void changeRecipe(GlueScheduleResult glueScheduleResult);

    /**
     * 根据密炼区、胶料名称，查询对应配方的机台信息
     *
     * @param mesPmtRecipe 密炼区、胶料名称
     * @return 对应配方的机台信息
     */
    List<MesPmtRecipe> selectMesPmtRecipeMachine(MesPmtRecipe mesPmtRecipe);

    /**
     * 根据密炼区和胶料名称查询配方与机台对应信息
     *
     * @param machine 参数
     * @return 查询到的集合
     */
    List<FormulaMachine> getFormulaMachineList(FormulaMachine machine);

    /**
     * 根据ids查询发布状态是否有不是【未发布】的记录
     * @param ids ids
     * @return 不是未发布的记录数
     */
    int isNoReleaseByIds(@Param("array") Long[] ids);

    /**
     * 获取统计信息
     * @param glueScheduleResult 日期、密炼区、机台编号
     * @return 统计好的信息列表
     */
    List<GlueScheduleResultStatisticsDto> statistics(GlueScheduleResult glueScheduleResult);

    /**
     * 根据id更新指定记录
     * @param glueScheduleResult 要更新的记录
     */
    void updateScheduleResult(GlueScheduleResult glueScheduleResult);

    /**
     * 根据id集合查询对应记录，修改后理论上不存在idList内元素过多的情况，不做分段查询处理
     * @param idList id集合
     * @return 查询到的记录
     */
    List<GlueScheduleResult> selectByIds(@Param("idList") List<Long> idList);

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     * @param glueScheduleResult 参数
     * @return 结果
     */
    GlueSpanReceiveQtyDto getSumQtyByMachineCode(GlueScheduleResult glueScheduleResult);

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    List<GlueScheduleResult> selectSpanSendNeedFieldByIds(Long[] ids);

    /**
     * 查询昨日排程早班计划汇总
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getLastDayPlanQty(GlueScheduleResult scheduleResult);

    /**
     * 查询终炼母炼夜班、早班、库存汇总
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getSummaryVo(GlueScheduleResult scheduleResult);

    /**
     * 查询消耗量，用于计算理论交接班库存
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    Double getConsume(GlueScheduleResult scheduleResult);
}
