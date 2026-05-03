package com.example.testapi.service;

import com.example.testapi.dto.AddMemberRequest;
import com.example.testapi.dto.CreateGroupRequest;
import com.example.testapi.dto.GroupResponse;
import com.example.testapi.dto.UserSummaryDto;
import com.example.testapi.entity.Group;
import com.example.testapi.entity.User;
import com.example.testapi.exception.BadRequestException;
import com.example.testapi.exception.ResourceNotFoundException;
import com.example.testapi.repository.GroupRepository;
import com.example.testapi.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsForUser(Long userId) {
        return groupRepository.findDistinctByMembersId(userId)
                .stream()
                .map(this::toGroupResponse)
                .toList();
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, Long currentUserId) {
        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Group group = new Group(request.getName());
        group.getMembers().add(creator);
        Group saved = groupRepository.save(group);

        return toGroupResponse(saved);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long groupId, Long currentUserId) {
        Group group = getGroupAndValidateMember(groupId, currentUserId);
        return toGroupResponse(group);
    }

    @Transactional
    public GroupResponse addMember(Long groupId, AddMemberRequest request, Long currentUserId) {
        Group group = getGroupAndValidateMember(groupId, currentUserId);

        User userToAdd = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Cannot add member because user does not exist"));

        group.getMembers().add(userToAdd);
        Group saved = groupRepository.save(group);

        return toGroupResponse(saved);
    }

    @Transactional(readOnly = true)
    public Group getGroupAndValidateMember(Long groupId, Long currentUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        boolean isMember = group.getMembers()
                .stream()
                .anyMatch(member -> member.getId().equals(currentUserId));

        if (!isMember) {
            throw new AccessDeniedException("You are not allowed to access this group");
        }

        return group;
    }

    private GroupResponse toGroupResponse(Group group) {
        List<UserSummaryDto> members = group.getMembers()
                .stream()
                .sorted(Comparator.comparing(User::getId))
                .map(user -> new UserSummaryDto(user.getId(), user.getName(), user.getEmail()))
                .collect(Collectors.toList());

        return new GroupResponse(group.getId(), group.getName(), members);
    }
}
