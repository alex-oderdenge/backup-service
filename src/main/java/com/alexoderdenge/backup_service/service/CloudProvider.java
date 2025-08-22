package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.service.exception.RcloneException;

public interface CloudProvider {
    //TODO refactor replacing isFile with an appropriate Class with all the necessary properties
    void backup(String source, String destination, boolean isFile) throws RcloneException;
}