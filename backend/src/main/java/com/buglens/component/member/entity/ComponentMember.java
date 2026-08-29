package com.buglens.component.member.entity;

import com.buglens.auth.entity.User;
import com.buglens.component.entity.Component;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "component_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_component_members_component_user",
                columnNames = {"component_id", "user_id"}
        )
)
public class ComponentMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private Component component;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComponentMemberRole role;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected ComponentMember() {
    }

    public ComponentMember(Component component, User user, ComponentMemberRole role) {
        this.component = component;
        this.user = user;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        assignedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Component getComponent() {
        return component;
    }

    public User getUser() {
        return user;
    }

    public ComponentMemberRole getRole() {
        return role;
    }

    public void changeRole(ComponentMemberRole role) {
        this.role = role;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
