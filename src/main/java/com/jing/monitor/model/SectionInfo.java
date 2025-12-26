package com.jing.monitor.model;

public class SectionInfo {
    private String subject;
    private String catalogNumber;
    private String section;
    private StatusMapping status;
    private String courseId;

    public SectionInfo() {}

    public SectionInfo(String subject, String catalogNumber, String classNumber, StatusMapping status, String courseId){
        this.subject = subject;
        this.catalogNumber = catalogNumber;
        this.section = classNumber;
        this.status = status;
        this.courseId = courseId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCatalogNumber() {
        return catalogNumber;
    }

    public void setCatalogNumber(String catalogNumber) {
        this.catalogNumber = catalogNumber;
    }

    public StatusMapping getStatus() {
        return status;
    }

    public void setStatus(StatusMapping status) {
        this.status = status;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    @Override
    public String toString() {
        return "CourseStatus{" +
                "subject='" + subject + '\'' +
                ", catalogNumber='" + catalogNumber + '\'' +
                ", section='" + section + '\'' +
                ", status=" + status +
                ", courseId='" + courseId + '\'' +
                '}';
    }
}