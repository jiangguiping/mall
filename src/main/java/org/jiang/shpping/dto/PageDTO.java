package org.jiang.shpping.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jiang.shpping.utils.Constants;

@Data
public class PageDTO {
    /**
     * 页码
     */
    @ApiModelProperty(name = "current", value = "页码", dataType = "Long")
    private int page = Constants.DEFAULT_PAGE;

    /**
     * 条数
     */
    @ApiModelProperty(name = "size", value = "条数", dataType = "Long")
    private int limit = Constants.DEFAULT_LIMIT;;

}
