package com.zlt.aps.tc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tc.api.domain.dto.TcGlueGroupOrderDto;
import com.zlt.aps.tc.entity.TcGlueGroupOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * <p>
 * 胎侧胶料组别顺序维护 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-25
 */
public interface TcGlueGroupOrderMapper extends BaseMapper<TcGlueGroupOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<TcGlueGroupOrderDto> listGlueGroupOrder(TcGlueGroupOrderDto dto);

    /**
     * 查询出被使用了的胶料组别
     *
     * @param glueGroupIds
     * @return
     */
    List<String> listUserdGlueGroup(@Param("glueGroupIds") List<Long> glueGroupIds);

    /**
     * 合并操作，存在则更新，否则新增
     */
    public void mergeSql(List<TcGlueGroupOrderDto> list);
}
