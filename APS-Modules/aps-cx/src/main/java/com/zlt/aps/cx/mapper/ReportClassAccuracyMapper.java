package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.dto.ReportClassAccuracyDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 班次完成统计报表Mapper接口
 *
 * @author chen
 * @date 2022-05-23
 */
public interface ReportClassAccuracyMapper {

    /**
     * 查询硫化班次准确率列表基础数据
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return
     */
    List<ReportClassAccuracyDto> listLhClassAccuracy(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询成型班次准确率列表基础数据
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return
     */
    List<ReportClassAccuracyDto> listCxClassAccuracy(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询胎面班次准确率列表基础数据
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return
     */
    List<ReportClassAccuracyDto> listTmClassAccuracy(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询胎侧班次准确率列表基础数据
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return
     */
    List<ReportClassAccuracyDto> listTcClassAccuracy(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询内衬班次准确率列表基础数据
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return
     */
    List<ReportClassAccuracyDto> listNcClassAccuracy(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询胎圈班次准确率列表基础数据
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return
     */
    List<ReportClassAccuracyDto> listTqClassAccuracy(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询钢丝圈班次准确率列表基础数据
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return
     */
    List<ReportClassAccuracyDto> listGsqClassAccuracy(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询15度裁断班次准确率列表基础数据
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return
     */
    List<ReportClassAccuracyDto> listCd15ClassAccuracy(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询90度裁断班次准确率列表基础数据
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return
     */
    List<ReportClassAccuracyDto> listCd90ClassAccuracy(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询纤维压延班次准确率列表基础数据
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return
     */
    List<ReportClassAccuracyDto> listXwyyClassAccuracy(@Param("scheduleDate") String scheduleDate);
}
