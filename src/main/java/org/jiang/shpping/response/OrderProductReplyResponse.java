package org.jiang.shpping.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="OrderProductReplyResponse对象", description="评价商品页响应对象")
public class OrderProductReplyResponse implements Serializable {

    private static final long serialVersionUID = -1926585407216207845L;

    @ApiModelProperty(value = "购买数量")
    private Integer cartNum;

    @ApiModelProperty(value = "价格")
    private BigDecimal truePrice;

    @ApiModelProperty(value = "商品名称")
    private String storeName;

    @ApiModelProperty(value = "图片")
    private String image;

    @ApiModelProperty(value = "商品编号")
    private Integer productId;

    @ApiModelProperty(value = "规格sku")
    private String sku;

}
