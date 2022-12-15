package org.jiang.shpping.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="PreOrderRequest对象", description="预下单详情请求对象")
public class PreOrderDetailRequest {

    @ApiModelProperty(value = "购物车编号，购物车预下单时必填")
    private Long shoppingCartId;

    @ApiModelProperty(value = "商品id（立即购买必填）")
    private Integer productId;

    @ApiModelProperty(value = "商品规格属性id（立即购买、活动购买必填）")
    private Integer attrValueId;

    @ApiModelProperty(value = "商品数量（立即购买、活动购买必填）")
    private Integer productNum;

    @ApiModelProperty(value = "订单编号（再次购买必填）")
    private String orderNo;



}
