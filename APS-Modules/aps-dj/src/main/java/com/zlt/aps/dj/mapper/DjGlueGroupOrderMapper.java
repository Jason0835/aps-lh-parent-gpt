package com.zlt.aps.dj.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.dj.api.domain.dto.DjGlueGroupOrderDto;
import com.zlt.aps.dj.api.domain.entity.DjGlueGroupOrder;


/**
 * <p>
 * 垫胶胶料组别顺序维护 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-25
 */
public interface DjGlueGroupOrderMapper extends BaseMapper<DjGlueGroupOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<DjGlueGroupOrderDto> listGlueGroupOrder(DjGlueGroupOrderDto dto);

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
    public void mergeSql(List<DjGlueGroupOrderDto> list);
}
