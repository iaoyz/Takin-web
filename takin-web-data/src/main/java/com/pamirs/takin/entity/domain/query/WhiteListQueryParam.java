package com.pamirs.takin.entity.domain.query;

import java.io.Serializable;

import lombok.Data;

/**
 * @author 慕白
 * @date 2020-03-05 11:10
 */

@Data
public class WhiteListQueryParam implements Serializable {

    private static final long serialVersionUID = 3987892771068578283L;

    private String applicationId;

    private Long id;

    private Integer currentPage;

    private Integer pageSize;

}
