package com.zlt.aps.monthplan.api.domain.itf;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Chen
 * @date 2025/4/8
 */
@Data
public class ListDataVo<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> data;

    private Integer totalPage;

    private Integer totalCount;
}
