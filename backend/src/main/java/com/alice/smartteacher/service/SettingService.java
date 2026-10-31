package com.alice.smartteacher.service;

import com.alice.smartteacher.model.AppSetting;
import com.alice.smartteacher.repo.AppSettingRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingService {
    private final AppSettingRepository settings;

    public SettingService(AppSettingRepository settings) {
        this.settings = settings;
    }

    public String get(String key, String fallback) {
        return settings.findBySettingKey(key).map(AppSetting::getSettingValue).orElse(fallback);
    }

    public boolean enabled(String key) {
        return !"false".equalsIgnoreCase(get(key, "true"));
    }

    public void put(String key, String value) {
        AppSetting row = settings.findBySettingKey(key).orElseGet(AppSetting::new);
        row.setSettingKey(key);
        row.setSettingValue(value);
        settings.save(row);
    }
}
