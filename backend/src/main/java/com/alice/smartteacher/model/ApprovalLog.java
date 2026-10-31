package com.alice.smartteacher.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "approval_logs")
public class ApprovalLog extends BaseEntity {

    @ManyToOne(optional = false)
    private SchoolDocument document;

    @ManyToOne(optional = false)
    private UserAccount reviewer;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(length = 2000)
    private String comment;
}
