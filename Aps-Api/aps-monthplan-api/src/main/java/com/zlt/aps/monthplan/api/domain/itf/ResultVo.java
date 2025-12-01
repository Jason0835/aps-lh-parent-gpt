package com.zlt.aps.monthplan.api.domain.itf;

import lombok.Data;

import java.io.Serializable;

/**
 * 结果Vo
 *
 * @author Chen
 * @date 2025/4/8
 */
@Data
public class ResultVo<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private T data;
}
