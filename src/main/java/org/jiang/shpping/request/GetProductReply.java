package org.jiang.shpping.request;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;


import javax.validation.constraints.NotNull;


@Data
public class GetProductReply {

    @ApiModelProperty(value = "商品attrid")
//    @NotBlank(message = "商品uniId不能为空")
    private String uni;

    @ApiModelProperty(value = "订单id")
    @NotNull(message = "订单id不能为空")
    private Integer orderId;
}
