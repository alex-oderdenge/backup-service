package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.service.exception.RemoteNotConfiguredException;
import com.alexoderdenge.backup_service.service.exception.RcloneNotInstalledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RcloneValidatorTest {

    @InjectMocks
    private RcloneValidator rcloneValidator;

    @Test
    void extractRemoteName_ValidFormat_ReturnsRemoteName() {
        String destination = "myRemote:/path/to/destination";
        String remoteName = rcloneValidator.extractRemoteName(destination);
        assertEquals("myRemote", remoteName);
    }

    @Test
    void extractRemoteName_InvalidFormat_ThrowsException() {
        String destination = "invalid-format";
        assertThrows(IllegalArgumentException.class, () -> {
            rcloneValidator.extractRemoteName(destination);
        });
    }

    @Test
    void extractRemoteName_NullDestination_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            rcloneValidator.extractRemoteName(null);
        });
    }

    @Test
    void extractRemoteName_EmptyDestination_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            rcloneValidator.extractRemoteName("");
        });
    }

    @Test
    void extractRemoteName_WhitespaceDestination_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            rcloneValidator.extractRemoteName("   ");
        });
    }
}
