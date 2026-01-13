package com.system.batch.business.domain;

import com.system.batch.config.T800ProtocolConfig;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Entity
@Table(name = "humans")
@Data
public class Human {
    @Id
    private Long id;
    private String name;
    //  저항군 내 계급 (COMMANDER, OFFICER, SOLDIER, CIVILIAN 등)
    private String rank;
    private Boolean terminated;

    @OneToMany(mappedBy = "human", fetch = FetchType.EAGER)
    @BatchSize(size = 100)
    private List<Activity> activities;
}
