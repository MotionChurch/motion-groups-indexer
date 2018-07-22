package com.p4square.groupsindexer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * A group description in ElasticSearch.
 */
public class GroupSearchDocument {

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("image-url")
    private String imageUrl;

    @JsonProperty("leader-id")
    private int leaderId;

    @JsonProperty("leader-name")
    private String leaderName;

    @JsonProperty("leader-email")
    private String leaderEmail;

    @JsonProperty("member-count")
    private int currentMembers;

    @JsonProperty("group-capacity")
    private Integer groupCapacity;

    @JsonProperty("childcare")
    private boolean childcareProvided;

    @JsonProperty("listed")
    private boolean listed;

    @JsonProperty("public")
    private boolean publicSearchListed;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("udf")
    private Map<String, Reference> customFields;

    private Reference campus;
    private Reference groupType;
    private Reference department;
    private Reference area;
    private Reference meetingDay;
    private Reference meetingTime;

    public GroupSearchDocument() {
        customFields = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public String getLeaderEmail() {
        return leaderEmail;
    }

    public void setLeaderEmail(String leaderEmail) {
        this.leaderEmail = leaderEmail;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getCurrentMembers() {
        return currentMembers;
    }

    public void setCurrentMembers(int currentMembers) {
        this.currentMembers = currentMembers;
    }

    public Integer getGroupCapacity() {
        return this.groupCapacity;
    }

    @JsonIgnore
    public boolean isGroupCapacityUnlimited() {
        return this.groupCapacity == null;
    }

    public void setGroupCapacity(Integer groupCapacity) {
        this.groupCapacity = groupCapacity;
    }

    public boolean isChildcareProvided() {
        return childcareProvided;
    }

    public void setChildcareProvided(boolean childcareProvided) {
        this.childcareProvided = childcareProvided;
    }

    public boolean isListed() {
        return listed;
    }

    public void setListed(boolean listed) {
        this.listed = listed;
    }

    public boolean isPublicSearchListed() {
        return publicSearchListed;
    }

    public void setPublicSearchListed(boolean publicSearchListed) {
        this.publicSearchListed = publicSearchListed;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Reference getCampus() {
        return campus;
    }

    public void setCampus(Reference campus) {
        this.campus = campus;
    }

    public Reference getGroupType() {
        return groupType;
    }

    public void setGroupType(Reference groupType) {
        this.groupType = groupType;
    }

    public Reference getDepartment() {
        return department;
    }

    public void setDepartment(Reference department) {
        this.department = department;
    }

    public Reference getArea() {
        return area;
    }

    public void setArea(Reference area) {
        this.area = area;
    }

    public Reference getMeetingDay() {
        return meetingDay;
    }

    public void setMeetingDay(Reference meetingDay) {
        this.meetingDay = meetingDay;
    }

    public Reference getMeetingTime() {
        return meetingTime;
    }

    public void setMeetingTime(Reference meetingTime) {
        this.meetingTime = meetingTime;
    }

    public Map<String, Reference> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Reference> customFields) {
        this.customFields = customFields;
    }

}