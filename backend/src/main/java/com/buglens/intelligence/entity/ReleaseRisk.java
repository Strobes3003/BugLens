package com.buglens.intelligence.entity;

import com.buglens.release.entity.Release;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "release_risks")
public class ReleaseRisk {

    @Id
    @Column(name = "release_id")
    private Long releaseId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "release_id")
    private Release release;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    protected ReleaseRisk() {
    }

    public ReleaseRisk(Release release, int riskScore) {
        this.release = release;
        this.riskScore = riskScore;
        this.calculatedAt = Instant.now();
    }

    public void recalculatedAs(int riskScore) {
        this.riskScore = riskScore;
        this.calculatedAt = Instant.now();
    }

    public Long getReleaseId() {
        return releaseId;
    }

    public Release getRelease() {
        return release;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }
}
