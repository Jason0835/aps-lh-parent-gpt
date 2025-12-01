package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.dto.MixingTimeDto;
import com.zlt.mix.setting.api.domain.entity.MixingTime;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 炼胶时间信息Mapper接口
 *
 * @author Liam
 * @date 2022-03-31
 */
public interface MixingTimeMapper extends BaseMapper<MixingTime> {

    /**
     * 查询炼胶时间信息列表
     * 拼接机台表获取机台名称，同时也要返回机台编号
     *
     * @param mixingTime 炼胶时间信息
     * @return 炼胶时间信息集合
     */
    List<MixingTimeDto> selectMixingTimeList(MixingTime mixingTime);

    /**
     * 批量删除炼胶时间信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteMixingTimeByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * 注意炼胶时间可以不指定机台信息
     * 如果不存在是表内的机台编号不存在，Excel中的机台名称（因为导入时输入的是机台编号）不存在
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listMixingTimeNotUnique(@Param("importList") List<MixingTimeDto> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertMixingTimeInfo(@Param("list") List<MixingTimeDto> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<MixingTimeDto> list);

    /**
     * 通过机台名称+密炼区进行批量获取机台编号
     *
     * @param list 炼胶时间信息Dto
     * @return 对应Excel列的机台编号，不存在的机台编号就返回为null
     */
    List<String> listMixMachineCode(@Param("importList") List<MixingTimeDto> list);
}
