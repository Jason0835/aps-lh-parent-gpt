package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.dto.MachineGlueDecomposeDto;
import com.zlt.mix.setting.api.domain.entity.MachineGlueDecompose;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 密炼机指定胶料分解Mapper接口
 *
 * @author Liam
 * @date 2022-03-29
 */
public interface MachineGlueDecomposeMapper extends BaseMapper<MachineGlueDecompose> {


    /**
     * 批量删除密炼机指定胶料分解
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteMachineGlueDecomposeByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listMachineGlueDecomposeNotUnique(@Param("importList") List<MachineGlueDecomposeDto> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertMachineGlueDecomposeInfo(@Param("list") List<MachineGlueDecomposeDto> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<MachineGlueDecomposeDto> list);

    /**
     * 查询密炼机指定胶料分解列表(级联查询机台名称)
     *
     * @param machineGlueDecompose 密炼机指定胶料分解
     * @return 密炼机指定胶料分解Dto
     */
    List<MachineGlueDecomposeDto> selectMachineGlueDecomposeListCascade(MachineGlueDecompose machineGlueDecompose);

    /**
     * 获取密炼机指定胶料分解详细信息(级联查询机台名称)
     *
     * @param id 密炼机指定胶料分解ID
     * @return 密炼机指定胶料分解Dto
     */
    MachineGlueDecomposeDto getByIdCascade(Long id);

    /**
     * 通过机台名称批量查询出机台编号
     * 为避免在使用右连接时，oracle优化引擎可能会导致顺序改变，指定了正确的顺序字段进行排序
     *
     * @param list 导入的数据列表
     * @return 机台名称列表
     */
    List<String> selectMachineCodeList(@Param("importList") List<MachineGlueDecomposeDto> list);
}
