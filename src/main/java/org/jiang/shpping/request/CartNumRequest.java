package org.jiang.shpping.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="CartRequest对象", description="购物车数量请求对象")
public class CartNumRequest implements Serializable {

    private static final long serialVersionUID = -1186533756329913311L;

    @ApiModelProperty(value = "数量类型：total-商品数量，sum-购物数量", required = true)
    @NotNull(message = "数量类型不能为空")
    private String type;

    @ApiModelProperty(value = "商品类型：true-有效商品，false-无效商品", required = true)
    @NotNull(message = "商品类型不能为空")
    private Boolean numType;

}
