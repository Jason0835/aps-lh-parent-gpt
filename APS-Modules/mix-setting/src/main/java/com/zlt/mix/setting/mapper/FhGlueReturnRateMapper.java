package com.zlt.mix.setting.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.FhGlueReturnRate;

/**
 * 返回胶日返回率Mapper接口
 * 
 * @author zlt
 * @date 2022-11-28
 */
public interface FhGlueReturnRateMapper extends BaseMapper<FhGlueReturnRate> {

    /**
     * 查询返回胶日返回率列表
     * 
     * @param fhGlueReturnRate 返回胶日返回率
     * @return 返回胶日返回率集合
     */
    List<FhGlueReturnRate> selectFhGlueReturnRateList(FhGlueReturnRate fhGlueReturnRate);

    /**
     * 批量删除返回胶日返回率
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteFhGlueReturnRateByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listFhGlueReturnRateNotUnique(@Param("importList") List<FhGlueReturnRate> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     * @param list
     */
    void batchInsertFhGlueReturnRateInfo(@Param("list") List<FhGlueReturnRate> list);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    void mergeSql(List<FhGlueReturnRate> list);
}
