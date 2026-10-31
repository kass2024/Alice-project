package com.alice.smartteacher.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_settings")
public class AppSetting extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String settingKey;

    @Column(columnDefinition = "TEXT")
    private String settingValue;
}
