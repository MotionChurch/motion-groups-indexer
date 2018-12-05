package com.p4square.groupsindexer.model;

import com.p4square.ccbapi.model.CustomPulldownFieldValue;
import com.p4square.ccbapi.model.GroupProfile;

import java.util.Arrays;
import java.util.function.Function;

/**
 * GroupSearchDocumentAdapter is a function which converts a CCB {@link GroupProfile} to a {@link GroupSearchDocument}.
 */
public class GroupSearchDocumentAdapter implements Function<GroupProfile, GroupSearchDocument> {
    @Override
    public GroupSearchDocument apply(GroupProfile groupProfile) {
        final GroupSearchDocument doc = new GroupSearchDocument();

        doc.setId(groupProfile.getId());
        doc.setName(groupProfile.getName());
        doc.setDescription(groupProfile.getDescription());
        doc.setImageUrl(groupProfile.getImageUrl());
        doc.setLeaderId(groupProfile.getMainLeader().getId());
        doc.setLeaderName(
                        groupProfile.getMainLeader().getFirstName() + " " +
                        abbreviateName(groupProfile.getMainLeader().getLastName()));
        if (groupProfile.getAddresses().size() > 0) {
            doc.setLocationCity(groupProfile.getAddresses().get(0).getCity());
        }
        doc.setCurrentMembers(groupProfile.getCurrentMembers());
        doc.setGroupCapacity(groupProfile.getGroupCapacity());
        doc.setChildcareProvided(groupProfile.isChildcareProvided());
        doc.setListed(groupProfile.isListed());
        doc.setPublicSearchListed(groupProfile.isPublicSearchListed());
        doc.setActive(groupProfile.isActive());
        doc.setCampus(adaptReference(groupProfile.getCampus()));
        doc.setGroupType(adaptReference(groupProfile.getGroupType()));
        doc.setDepartment(adaptReference(groupProfile.getDepartment()));
        doc.setArea(adaptReference(groupProfile.getArea()));
        doc.setMeetingDay(adaptReference(groupProfile.getMeetingDay()));
        doc.setMeetingTime(adaptReference(groupProfile.getMeetingTime()));

        for (final CustomPulldownFieldValue field : groupProfile.getCustomPulldownFields()) {
            final Reference ref = new Reference();
            ref.setId(String.valueOf(field.getSelection().getId()));
            ref.setLabel(field.getSelection().getLabel());
            doc.getCustomFields().put(field.getName(), ref);
        }

        return doc;
    }

    private Reference adaptReference(com.p4square.ccbapi.model.Reference r) {
        final Reference ref = new Reference();
        ref.setId(String.valueOf(r.getId()));
        ref.setLabel(r.getName());
        return ref;
    }

    private String abbreviateName(final String name) {
        return Arrays.stream(name.split(" "))
                .map(s -> s.substring(0, 1) + ".")
                .reduce("", String::concat);
    }
}
