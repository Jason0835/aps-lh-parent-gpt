package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.MesLhScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 硫化排程结果下发Mapper接口
 *
 * @author APS Team
 * @since 2.0.0
 */
@DS(DataSource.MES)
@Mapper
public interface LhScheduleResultIssueMapper {

    /**
     * 批量新增硫化排程结果到中间表
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchInsertLhScheduleResult(@Param("list") List<MesLhScheduleResult> list);

    /**
     * 根据排程日期和机台更新数据
     *
     * @param mesItem 数据项
     * @return 影响行数
     */
    int updateByScheduleDateAndMachine(MesLhScheduleResult mesItem);

    /**
     * 根据排程日期删除数据
     *
     * @param scheduleDate 排程日期
     * @param dataVersion 版本号
     * @return 影响行数
     */
    int deleteByScheduleDate(@Param("scheduleDate") String scheduleDate, 
                             @Param("dataVersion") String dataVersion);
}
