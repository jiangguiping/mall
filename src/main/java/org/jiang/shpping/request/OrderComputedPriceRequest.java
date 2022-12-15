package org.jiang.shpping.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="OrderComputedPriceRequest对象", description="计算订单价格请求对象")
public class OrderComputedPriceRequest {

    @ApiModelProperty(value = "预下单订单号")
    private String preOrderNo;

    @ApiModelProperty(value = "地址id")
    private Integer addressId;

    @ApiModelProperty(value = "优惠券id")
    private Integer couponId;

    @ApiModelProperty(value = "快递类型: 1-快递配送，2-到店自提")
    private Integer shippingType;

    @ApiModelProperty(value = "是否使用积分")
    private Boolean useIntegral;

}
