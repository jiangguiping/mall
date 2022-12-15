package org.jiang.shpping.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="UserSignInfoResponse对象", description="用户签到信息响应对象")
public class UserSignInfoResponse implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "连续签到天数")
    private Integer signNum;

    @ApiModelProperty(value = "累计签到次数")
    private Integer sumSignDay;

    @ApiModelProperty(value = "今天是否签到")
    private Boolean isDaySign;

    @ApiModelProperty(value = "昨天是否签到")
    private Boolean isYesterdaySign;

    @ApiModelProperty(value = "用户积分")
    private Integer integral;

}
