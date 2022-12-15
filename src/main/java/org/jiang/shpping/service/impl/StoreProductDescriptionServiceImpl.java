package org.jiang.shpping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.StoreProductDescriptionDao;
import org.jiang.shpping.entity.StoreProductDescription;
import org.jiang.shpping.service.StoreProductDescriptionService;
import org.springframework.stereotype.Service;

@Service
public class StoreProductDescriptionServiceImpl extends ServiceImpl<StoreProductDescriptionDao, StoreProductDescription> implements StoreProductDescriptionService {
}
