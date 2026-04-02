package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmStructureTreadConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * APS结构整车胎面配置Mapper
 *
 * @author zlt
 * @since 2025/12/25
 */
@Mapper
public interface MdmStructureTreadConfigEntityMapper extends BaseMapper<MdmStructureTreadConfig> {

    /**
     * 根据唯一键查询已存在的数据
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<MdmStructureTreadConfig> selectByUniqueKeyList(@Param("list") List<MdmStructureTreadConfig> list);

}
