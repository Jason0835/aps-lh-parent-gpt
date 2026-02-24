package com.zlt.aps.mdm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmVulcanizingMachStatus;
import com.zlt.aps.mdm.api.domain.vo.CopyParamVo;
import com.zlt.aps.mdm.api.domain.vo.MdmVulcanizingMachStatusVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 硫化机状态
 */
@Mapper
public interface MdmVulcanizingMachStatusEntityMapper extends BaseMapper<MdmVulcanizingMachStatus> {

    /**
     * 查询硫化机可用信息列表
     *
     * @param entity 查询条件
     * @return 查询到的结果
     */
    public List<MdmVulcanizingMachStatusVo> selectDocVulcanizingMachStatusEntityList(MdmVulcanizingMachStatusVo entity);

    /**
     * 校验唯一性
     *
     * @param entity 记录
     * @return 查询到的记录条数
     */
    public int checkUnique(MdmVulcanizingMachStatusVo entity);

    /**
     * 根据id数组更新状态
     *
     * @param ids    id
     * @param status 状态
     * @return 结果
     */
    public int updateDocVulcanizingMachStatusListById(@Param("list") List<Long> ids, @Param("status") String status);

    /**
     * 根据年月删除记录
     *
     * @param copyParamVo 复制参数
     * @return
     */
    public int deleteByYearAndMonth(CopyParamVo copyParamVo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    int mergeSql(Collection<? extends MdmVulcanizingMachStatus> collection);

    /**
     * 根据硫化机生成可用状态列表
     */
    List<MdmVulcanizingMachStatus> selectGenerateListByMachine(CopyParamVo params);
}