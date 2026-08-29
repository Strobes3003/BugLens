package com.buglens.intelligence.entity;

import com.buglens.component.entity.Component;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "component_healths")
public class ComponentHealth {

    @Id
    @jakarta.persistence.Column(name = "component_id")
    private Long componentId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id")
    private Component component;

    @jakarta.persistence.Column(name = "health_score", nullable = false)
    private int healthScore;

    @jakarta.persistence.Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    protected ComponentHealth() {
    }

    public ComponentHealth(Component component, int healthScore) {
        this.component = component;
        this.healthScore = healthScore;
        this.calculatedAt = Instant.now();
    }

    public void recalculatedAs(int healthScore) {
        this.healthScore = healthScore;
        this.calculatedAt = Instant.now();
    }

    public Long getComponentId() {
        return componentId;
    }

    public Component getComponent() {
        return component;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }
}
