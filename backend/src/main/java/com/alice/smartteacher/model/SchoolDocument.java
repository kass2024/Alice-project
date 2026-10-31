package com.alice.smartteacher.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "school_documents")
public class SchoolDocument extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Enums.DocCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.Track track = Enums.Track.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.DocStatus status = Enums.DocStatus.DRAFT;

    private String filePath;
    private String originalName;
    private String contentType;
    private int version = 1;

    @ManyToOne(optional = false)
    private UserAccount owner;

    @Column(length = 2000)
    private String reviewerComment;

    private String routedTo;

    @Column(columnDefinition = "LONGTEXT")
    private String analysisJson;

    private Instant updatedAt;
}
