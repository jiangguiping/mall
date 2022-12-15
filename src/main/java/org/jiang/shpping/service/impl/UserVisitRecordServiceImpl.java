package org.jiang.shpping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.UserVisitRecordDao;
import org.jiang.shpping.entity.UserVisitRecord;
import org.jiang.shpping.service.UserVisitRecordService;
import org.springframework.stereotype.Service;

@Service
public class UserVisitRecordServiceImpl extends ServiceImpl<UserVisitRecordDao, UserVisitRecord> implements UserVisitRecordService {}
