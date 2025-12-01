package com.zlt.aps.common.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.common.core.domain.IFinishQtyImport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 半部件各个工序完成量导入基础服务类
 *
 * @author zlt
 */
@Mapper
public interface BaseFinishQtyImportMapper extends BaseMapper<IFinishQtyImport> {

    /**
     * 根据日期删除半部件各个工序完成量
     *
     * @param tableName    表名
     * @param scheduleDate 排程日期
     * @return 结果
     */
    int removeDayFinishQtyByDate(@Param("tableName") String tableName, @Param("scheduleDate") Date scheduleDate);

    /**
     * 批量保存半部件各个工序完成量
     *
     * @param tableName            表名
     * @param list                 要保存的半部件各个工序完成量
     * @param codeColumn            字段编号1
     * @param codeColumn1           字段编号2
     * @param class1FinishQtyColumn 夜班次完成量字段名称
     * @param class2FinishQtyColumn 早班次完成量字段名称
     * @return 结果
     */
    int saveDayFinishQtyList(@Param("tableName") String tableName,
                             @Param("list") List<? extends IFinishQtyImport> list,
                             @Param("codeColumn") String codeColumn,
                             @Param("codeColumn1") String codeColumn1,
                             @Param("class1FinishQtyColumn") String class1FinishQtyColumn,
                             @Param("class2FinishQtyColumn") String class2FinishQtyColumn
    );

    /**
     * 根据半部件各个工序完成量保存半部件各个工序完成量汇总表
     * @param tableName 汇总表表名
     * @param totalTableName 汇总表表名
     * @param codeColumn            字段编号1
     * @param codeColumn1           字段编号2
     * @param class1FinishQtyColumn 夜班次完成量字段名称
     * @param class2FinishQtyColumn 早班次完成量字段名称
     * @return 结果
     */
    int saveDayFinishQtyTotalByDayFinish(@Param("tableName") String tableName,
                                         @Param("totalTableName") String totalTableName,
                                         @Param("codeColumn") String codeColumn,
                                         @Param("codeColumn1") String codeColumn1,
                                         @Param("class1FinishQtyColumn") String class1FinishQtyColumn,
                                         @Param("class2FinishQtyColumn") String class2FinishQtyColumn
    );
}
