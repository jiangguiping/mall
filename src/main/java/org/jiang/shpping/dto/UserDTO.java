package org.jiang.shpping.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jiang.shpping.entity.User;

import java.math.BigDecimal;

@Data
public class UserDTO{

    @ApiModelProperty(value = "用户id")
    @TableId(value = "uid", type = IdType.AUTO)
    private Integer uid;

    @ApiModelProperty(value = "用户昵称")
    private String nickname;

    @ApiModelProperty(value = "用户头像")
    private String avatar;

    @ApiModelProperty(value = "连续签到天数")
    private Integer signNum;

    @ApiModelProperty(value = "用户剩余积分")
    private Integer integral;

    @ApiModelProperty(value = "用户余额")
    private BigDecimal nowMoney;

    @ApiModelProperty(value = "用户剩余经验")
    private Integer experience;

    @ApiModelProperty(value = "等级")
    private Integer level;

}
