package org.jiang.shpping.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jiang.shpping.entity.StoreProduct;
import org.jiang.shpping.entity.StoreProductAttr;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="ProductDetailResponse对象", description="商品详情H5")
public class ProductDetailResponse implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "产品属性")
    private List<StoreProductAttr> productAttr;

    @ApiModelProperty(value = "商品属性详情")
    private HashMap<String, Object> productValue;

    @ApiModelProperty(value = "返佣金额区间")
    private String priceName;


    @ApiModelProperty(value = "商品信息")
    private StoreProduct productInfo;

    @ApiModelProperty(value = "收藏标识")
    private Boolean userCollect;
}
