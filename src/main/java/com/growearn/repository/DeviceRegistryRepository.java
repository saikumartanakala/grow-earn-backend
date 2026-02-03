package com.growearn.repository;

import com.growearn.entity.DeviceRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviceRegistryRepository extends JpaRepository<DeviceRegistry, Long> {
    Optional<DeviceRegistry> findByDeviceFingerprint(String deviceFingerprint);
    void deleteByDeviceFingerprint(String deviceFingerprint);
}
