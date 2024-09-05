package com.zlt.aps.tm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.api.domain.dto.TmGlueGroupOrderDto;
import com.zlt.aps.tm.entity.TmGlueGroupOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * <p>
 * 胎面胶料组别顺序维护 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-25
 */
public interface TmGlueGroupOrderMapper extends BaseMapper<TmGlueGroupOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<TmGlueGroupOrderDto> listGlueGroupOrder(TmGlueGroupOrderDto dto);

    /**
     * 查询出被使用了的胶料组别
     *
     * @param glueGroupIds
     * @return
     */
    List<String> listUserdGlueGroup(@Param("glueGroupIds") List<Long> glueGroupIds);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     * @param list 要合并的集合
     */
    public void mergeSql(List<TmGlueGroupOrderDto> list);
}
