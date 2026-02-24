package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineStatus;
import com.zlt.aps.monthplan.api.domain.vo.CopyParamVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmMoldingMachineStatusVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MdmMoldingMachineStatusEntityMapper extends BaseMapper<MdmMoldingMachineStatus> {

    /**
     * 查询基础数据-成型机可用信息列表
     *
     * @param docMoldingMachineStatus 基础数据-成型机可用信息
     * @return 基础数据-成型机可用信息集合
     */
    List<MdmMoldingMachineStatusVo> selectDocMoldingMachineStatusList(MdmMoldingMachineStatus docMoldingMachineStatus);

    /**
     * 校验唯一性
     *
     * @param entity 记录
     * @return 查询到的记录条数
     */
    int checkUnique(MdmMoldingMachineStatus entity);

    /**
     * 根据id数组更新状态
     *
     * @param ids    id
     * @param status 状态
     * @return 结果
     */
    int updateMoldingMachineStatusListById(@Param("list") List<Long> ids, @Param("status") String status);

    /**
     * 根据年月删除记录
     *
     * @param params 参数
     * @return 影响行数
     */
    int deleteByYearAndMonth(CopyParamVo params);

    /**
     * 根据成型机生成成型机可用状态列表
     */
    List<MdmMoldingMachineStatus> selectGenerateListByMachine(CopyParamVo params);

}