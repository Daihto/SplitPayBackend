package com.example.testapi.dto;

import java.util.ArrayList;
import java.util.List;

public class GroupResponse {

    private Long id;
    private String name;
    private List<UserSummaryDto> members = new ArrayList<>();

    public GroupResponse() {
    }

    public GroupResponse(Long id, String name, List<UserSummaryDto> members) {
        this.id = id;
        this.name = name;
        this.members = members;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UserSummaryDto> getMembers() {
        return members;
    }

    public void setMembers(List<UserSummaryDto> members) {
        this.members = members;
    }
}
