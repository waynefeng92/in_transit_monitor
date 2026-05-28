package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.UploadBatch;
import com.company.roro.mapper.UploadBatchMapper;
import com.company.roro.service.UploadBatchService;
import org.springframework.stereotype.Service;

@Service
public class UploadBatchServiceImpl extends ServiceImpl<UploadBatchMapper, UploadBatch> implements UploadBatchService {
}