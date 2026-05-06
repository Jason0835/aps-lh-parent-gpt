package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmModelInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 模具台账 Mapper（MyBatis-Plus）
 */
@Mapper
public interface MdmModelInfoMapper extends BaseMapper<MdmModelInfo> {

    /**
     * 查询去重后的模套型号列表
     * @return 模套型号列表
     */
    List<String> listDistinctMouldSleeve();
}
