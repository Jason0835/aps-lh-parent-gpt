package com.zlt.aps.lh.mapper;

import java.util.Date;

import com.zlt.aps.lh.api.domain.entity.LhTestNewTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.lh.api.domain.entity.LhTestScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;


/**
 * Description: 硫化TEST排程结果Mapper
 * @author
 */
@Mapper
public interface LhTestNewTableEntityMapper extends CommBaseMapper<LhTestNewTable> {
    /**
     * 根据分厂编码和排程时间删除记录
     *
     * @return 受影响的行数
     */
    int updateEmbryoCode1();

}
