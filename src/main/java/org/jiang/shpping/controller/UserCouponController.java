package org.jiang.shpping.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.jiang.shpping.service.StoreCouponUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/front/coupon")
@Api(tags = "营销 -- 优惠券")
public class UserCouponController {
    @Autowired
    private StoreCouponUserService storeCouponUserService;


}
