package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.service.exception.RcloneException;

public interface CloudProvider {
    void backup(String source, String destination) throws RcloneException;
}