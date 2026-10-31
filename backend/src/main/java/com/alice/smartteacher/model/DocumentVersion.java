package com.alice.smartteacher.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "document_versions")
public class DocumentVersion extends BaseEntity {

    @ManyToOne(optional = false)
    private SchoolDocument document;

    private int versionNumber;
    private String filePath;
    private String originalName;
    private String note;
}
