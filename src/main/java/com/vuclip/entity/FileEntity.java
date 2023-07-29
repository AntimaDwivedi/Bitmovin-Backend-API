package com.vuclip.entity;

public class FileEntity {

    private String cids;
    private String task;
    private String userEmail;

    public FileEntity() {
    }

    public String getCids() {
        return cids;
    }

    public String getTask() {
        return task;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setCids(String cids) {
        this.cids = cids;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    @Override
    public String toString() {
        return "FileEntity{" + "cids='" + cids + '\'' + ", task='" + task + '\'' + ", userEmail='" + userEmail + '\'' + '}';
    }
}
