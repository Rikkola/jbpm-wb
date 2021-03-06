/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.workbench.cm.client.roles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.user.client.TakesValue;
import org.jbpm.workbench.cm.client.util.AbstractCaseInstancePresenter;
import org.jbpm.workbench.cm.client.util.CaseRolesAssignmentFilterBy;
import org.jbpm.workbench.cm.client.util.CaseRolesValidations;
import org.jbpm.workbench.cm.model.CaseDefinitionSummary;
import org.jbpm.workbench.cm.model.CaseInstanceSummary;
import org.jbpm.workbench.cm.model.CaseRoleAssignmentSummary;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.UberElement;
import org.uberfire.mvp.Command;

import static java.util.stream.Collectors.toList;
import static org.jbpm.workbench.cm.client.resources.i18n.Constants.*;

@Dependent
@WorkbenchScreen(identifier = CaseRolesPresenter.SCREEN_ID)
public class CaseRolesPresenter extends AbstractCaseInstancePresenter<CaseRolesPresenter.CaseRolesView> {

    public static final String SCREEN_ID = "Case Roles";
    public static final String CASE_OWNER_ROLE = "owner";

    @Inject
    CaseRolesValidations caseRolesValidations;

    List<CaseRoleAssignmentSummary> allUnfilteredElements = new ArrayList();

    @Inject
    private EditRoleAssignmentView editRoleAssignmentView;

    private CaseDefinitionSummary caseDefinition;

    @WorkbenchPartTitle
    public String getTittle() {
        return translationService.format(ROLES);
    }

    @Override
    protected void clearCaseInstance() {
        view.removeAllRoles();
        allUnfilteredElements = new ArrayList();
        view.setBadge("0");
    }

    @Override
    protected void loadCaseInstance(final CaseInstanceSummary cis) {
        setupExistingAssignments(cis);
    }

    protected void setupExistingAssignments(final CaseInstanceSummary cis) {
        caseService.call(
                (CaseDefinitionSummary cds) -> {
                    if (cds == null || cds.getRoles() == null || cds.getRoles().isEmpty()) {
                        return;
                    }
                    caseDefinition = cds;
                    allUnfilteredElements = cds.getRoles()
                            .keySet()
                            .stream()
                            .map(roleName -> getRoleAssignment(cis,
                                                               roleName)).collect(toList());

                    view.setBadge(String.valueOf(allUnfilteredElements.size()));
                    filterElements();
                }
        ).getCaseDefinition(serverTemplateId,
                            containerId,
                            cis.getCaseDefinitionId());
    }

    public void filterElements() {
        String filterBy = view.getFilterValue();

        if (filterBy.equals(CaseRolesAssignmentFilterBy.ASSIGNED.getLabel())) {
            view.setRolesAssignmentList(
                    allUnfilteredElements.stream()
                            .filter(caseRoleAssignmentSummary -> caseRoleAssignmentSummary.hasAssignment())
                            .collect(toList()));
        } else if (filterBy.equals(CaseRolesAssignmentFilterBy.UNASSIGNED.getLabel())) {
            view.setRolesAssignmentList(
                    allUnfilteredElements.stream()
                            .filter(caseRoleAssignmentSummary -> !caseRoleAssignmentSummary.hasAssignment())
                            .collect(toList()));
        } else {
            view.setRolesAssignmentList(allUnfilteredElements);
        }
    }

    public void editAction(final CaseRoleAssignmentSummary caseRoleAssignmentSummary) {

        editRoleAssignmentView.setValue(CaseRoleAssignmentSummary.builder()
                                                .name(caseRoleAssignmentSummary.getName())
                                                .users(caseRoleAssignmentSummary.getUsers())
                                                .groups(caseRoleAssignmentSummary.getGroups()).build());
        editRoleAssignmentView.show(() -> assignToRole(caseRoleAssignmentSummary));
    }

    protected CaseRoleAssignmentSummary getRoleAssignment(final CaseInstanceSummary cis,
                                                          final String roleName) {
        if (CASE_OWNER_ROLE.equals(roleName)) {
            return CaseRoleAssignmentSummary.builder().name(roleName).users(Arrays.asList(cis.getOwner())).build();
        }
        return cis.getRoleAssignments()
                .stream()
                .filter(ra -> roleName.equals(ra.getName()))
                .findFirst().orElse(CaseRoleAssignmentSummary.builder().name(roleName).build());
    }

    protected void assignToRole(final CaseRoleAssignmentSummary previousAssignment) {
        final List<String> assignmentErrors =
                caseRolesValidations.validateRolesAssignments(caseDefinition,
                                                              Arrays.asList(editRoleAssignmentView.getValue()));
        if (assignmentErrors.isEmpty() == false) {
            editRoleAssignmentView.showValidationError(assignmentErrors);
            return;
        }
        editRoleAssignmentView.hide();
        storeRoleAssignments(previousAssignment,
                             editRoleAssignmentView.getValue().getUsers(),
                             editRoleAssignmentView.getValue().getGroups());
    }

    protected void storeRoleAssignments(final CaseRoleAssignmentSummary prevCaseRoleAssignmentSummary,
                                        List<String> users,
                                        List<String> groups) {
        List<String> prevUserAssignments = prevCaseRoleAssignmentSummary.getUsers();
        List<String> prevGroupsAssignments = prevCaseRoleAssignmentSummary.getGroups();
        List<String> newUserAssignments = users.stream().distinct().collect(toList());
        List<String> newGroupsAssignments = groups.stream().distinct().collect(toList());

        prevCaseRoleAssignmentSummary.setUsers(new ArrayList<String>(newUserAssignments));
        prevCaseRoleAssignmentSummary.setGroups(new ArrayList<String>(newGroupsAssignments));
        filterElements();

        List<String> usersToRemove = new ArrayList<String>(prevUserAssignments);
        usersToRemove.removeAll(newUserAssignments);
        usersToRemove.forEach(
                user -> caseService.call().removeUserFromRole(serverTemplateId,
                                                              containerId,
                                                              caseId,
                                                              prevCaseRoleAssignmentSummary.getName(),
                                                              user));

        newUserAssignments.removeAll(prevUserAssignments);
        newUserAssignments.stream().forEach(
                user -> caseService.call().assignUserToRole(serverTemplateId,
                                                            containerId,
                                                            caseId,
                                                            prevCaseRoleAssignmentSummary.getName(),
                                                            user));

        List<String> groupsToRemove = new ArrayList<String>(prevGroupsAssignments);
        groupsToRemove.removeAll(newGroupsAssignments);
        groupsToRemove.forEach(
                group -> caseService.call().removeGroupFromRole(serverTemplateId,
                                                                containerId,
                                                                caseId,
                                                                prevCaseRoleAssignmentSummary.getName(),
                                                                group));

        newGroupsAssignments.removeAll(prevGroupsAssignments);
        newGroupsAssignments.stream().forEach(
                group -> caseService.call().assignGroupToRole(serverTemplateId,
                                                              containerId,
                                                              caseId,
                                                              prevCaseRoleAssignmentSummary.getName(),
                                                              group));
    }

    protected void removeUserFromRole(final String userName,
                                      final CaseRoleAssignmentSummary caseRoleAssignmentSummary) {
        caseService.call(
                (Void) -> {
                    List users = caseRoleAssignmentSummary.getUsers();
                    users.remove(userName);
                    caseRoleAssignmentSummary.setUsers(users);
                    filterElements();
                }
        ).removeUserFromRole(serverTemplateId,
                             containerId,
                             caseId,
                             caseRoleAssignmentSummary.getName(),
                             userName);
    }

    protected void removeGroupFromRole(final String groupName,
                                       final CaseRoleAssignmentSummary caseRoleAssignmentSummary) {
        caseService.call(
                (Void) -> {
                    List groups = caseRoleAssignmentSummary.getGroups();
                    groups.remove(groupName);
                    caseRoleAssignmentSummary.setGroups(groups);
                    filterElements();
                }
        ).removeGroupFromRole(serverTemplateId,
                              containerId,
                              caseId,
                              caseRoleAssignmentSummary.getName(),
                              groupName);
    }

    public interface CaseRolesView extends UberElement<CaseRolesPresenter> {

        void removeAllRoles();

        void setRolesAssignmentList(List<CaseRoleAssignmentSummary> caseRoleAssignmentSummaryList);

        void resetPagination();

        String getFilterValue();

        void setBadge(String badgeContent);
    }

    public interface EditRoleAssignmentView extends UberElement<CaseRolesPresenter>,
                                                    TakesValue<CaseRoleAssignmentSummary> {

        void show(Command command);

        void showValidationError(List<String> messages);

        void hide();
    }

    public interface CaseRoleAction extends Command {

        String label();

        boolean isEnabled();
    }

    public interface CaseAssignmentItem extends Command {

        String label();
    }
}