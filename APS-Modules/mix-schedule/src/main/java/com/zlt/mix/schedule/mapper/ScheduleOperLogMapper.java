package com.zlt.mix.schedule.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import org.apache.ibatis.annotations.Param;
import com.zlt.mix.schedule.api.domain.entity.ScheduleOperLog;

/**
 * 排程操作日志Mapper接口
 *
 * @author chen
 * @date 2022-07-13
 */
public interface ScheduleOperLogMapper extends BaseMapper<ScheduleOperLog> {

    /**
     * 查询排程操作日志列表
     *
     * @param scheduleOperLog 排程操作日志
     * @return 排程操作日志集合
     */
    List<ScheduleOperLog> selectScheduleOperLogList(ScheduleOperLog scheduleOperLog);

    /**
     * 批量新增
     *
     * @param list 要新增的集合数据
     */
    void batchInsertScheduleOperLogInfo(@Param("list") List<ScheduleOperLog> list);

    /**
     * 查询配方类型列表
     *
     * @param recipeType 配方类型
     * @return 配方类型集合
     */
    List<RecipeType> selectRecipeTypeList(RecipeType recipeType);
}
