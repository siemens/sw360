/*
SPDX-FileCopyrightText: © 2023-24 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.listener;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.keycloak.event.listener.service.Sw360UserService;
import org.eclipse.sw360.keycloak.event.model.Group;
import org.eclipse.sw360.keycloak.event.model.UserEntity;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.userprofile.DefaultUserProfile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomEventListenerSW360 implements EventListenerProvider {

    private static final String REALM = "sw360";

    private static final String CUSTOM_ATTR_DEPARTMENT = "department";

    private static final Logger log = Logger.getLogger(CustomEventListenerSW360.class);
    public static final String USERNAME = "username";
    private final ObjectMapper objectMapper;

    Sw360UserService userService = new Sw360UserService();

    KeycloakSession keycloakSession;

    public CustomEventListenerSW360(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
        this.objectMapper = new ObjectMapper();
    }

    public void onEvent(Event event) {
        log.info("CustomEventListenerSW360:::onEvent(_) called!");
        log.info("Event client Id: " + event.getClientId());
        log.info("Event Type: " + event.getType());
        if (EventType.REGISTER.equals(event.getType())) {
            userRegistrationEvent(event);
        } else if (EventType.LOGIN.equals(event.getType())) {
            userLoginEvent(event);
        }
        log.info("CustomEventListenerSW360:::Exiting onEvent(_)::");
    }

    private void userLoginEvent(Event event) {
        log.info("Login Event called!");
        UserProvider userProvider = keycloakSession.users();
        RealmModel realmModel = keycloakSession.realms().getRealmByName(REALM);
        UserModel userModel = null;
        Map<String, String> details = event.getDetails();
        if (realmModel != null && details != null) {
            if (details.containsKey(USERNAME)) {
                String userName = details.get(USERNAME);
                if (userProvider != null) {
                    if (isValidEmail(userName)) {
                        userModel = userProvider.getUserByEmail(realmModel, userName);
                    } else {
                        userModel = userProvider.getUserById(realmModel, userName);
                    }
                }
            }
        }
        User user = new User();
        user.setEmail(userModel.getEmail());
        user.setFullname(userModel.getFirstName() + " " + userModel.getLastName());
        user.setExternalid(userModel.getUsername());
        user.setGivenname(userModel.getFirstName());
        user.setLastname(userModel.getLastName());
        log.info("User Model Attributes" + userModel.getAttributes());
        List<String> departments = userModel.getAttributes().getOrDefault("Department", Collections.singletonList("DEPARTMENT"));
        String department = departments.stream().findFirst().get();
        String parentDepartment = sanitizeDepartment(department);
        user.setDepartment(parentDepartment);
        boolean isUserExists = checkIfUserAlreadyExists(user);
        if (!isUserExists) {
            log.info("User logging in for the first time. Saving the user.." + user.getEmail());
            userService.addUser(user);
        }
        log.info("Login Event exited!");
    }

    private String sanitizeDepartment(String department) {
        if (department == null) {
            return department;
        } else {
            return department = department.trim().split("\\s+")[0];
        }
    }

    private boolean checkIfUserAlreadyExists(User user) {
        try {
            user = userService.getUserByEmail(user.getEmail());
        } catch (Exception ex) {
            log.error("User doesn't exist in the db!", ex);
            return false;
        }
        return user != null;
    }

    public boolean isValidEmail(String email) {
        String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private void writeUserToDB(Event event) {
        User user = new User();
        Map<String, String> details = event.getDetails();
        log.info("Event Details" + details);
        user = setUserDepartmentFromSession(user);
        user.setEmail(details.get("email"));
        user.setFullname(details.get("first_name") + " " + details.get("last_name"));
        user.setExternalid(details.get("username"));
        user.setGivenname(details.get("first_name"));
        user.setLastname(details.get("last_name"));
        userService.addUser(user);
    }

    private User setUserDepartmentFromSession(User user) {
        log.info("setUserDepartmentFromSession(_) called!");
        keycloakSession.getAttributes().entrySet().forEach(x -> {
            Object up = x.getValue();
            log.info("Session attribute" + up);
            if (up instanceof DefaultUserProfile usPro) {
                usPro.getAttributes().toMap().entrySet().forEach(y -> {
                    log.info("UserProfile from Session Key--->" + y.getKey());
                    log.info("UserProfile from Session Value--->" + y.getValue());
                    if (y.getKey().equalsIgnoreCase(CUSTOM_ATTR_DEPARTMENT) && !y.getValue().isEmpty()) {
                        user.setDepartment(y.getValue().get(0));
                    }
                });
            }
        });
        return user;
    }

    private void userRegistrationEvent(Event event) {
        writeUserToDB(event);
    }

    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        log.info("CustomEventListenerSW360:::onEvent(_,_) called!" + event.toString());
        log.info("onEvent() -->Resource Type:::" + event.getResourceType());
        if (ResourceType.USER.equals(event.getResourceType())) {
            if (OperationType.CREATE.equals(event.getOperationType())) {
                createUserOperation(event);
            } else if (OperationType.UPDATE.equals(event.getOperationType())) {
                updateUserOperation(event);
            } else if (OperationType.ACTION.equals(event.getOperationType())) {
                actionUserOpration(event);
            } else {
                log.info("User Details:::(? Event)" + event.getOperationType());
            }
        } else if (ResourceType.GROUP_MEMBERSHIP.equals(event.getResourceType())) {
            groupMembershipOperationAdminEvent(event);

        }
    }

    private void groupMembershipOperationAdminEvent(AdminEvent event) {
        log.info("Event Resource path" + event.getResourcePath());
        String resourcePath = event.getResourcePath();
        UserModel userModel = getUserModelFromSession(resourcePath);
        if (userModel.getGroupsStream().count() > 1) {
            throw new RuntimeException("User can not have multiple groups.");
        }
        log.info("Email--->: " + userModel.getEmail());
        if (OperationType.UPDATE.equals(event.getOperationType())) {
            log.info("Group Details:::(Group Membership Event)" + event.getRepresentation());
        } else if (OperationType.CREATE.equals(event.getOperationType())) {
            log.info("Group Details:::(Group Membership Event - CREATE)" + event.getRepresentation());
            Group userGroupModel = null;
            try {
                userGroupModel = objectMapper.readValue(event.getRepresentation(), Group.class);
                String userGroup = userGroupModel.getName();
                User userfromSw360DB = userService.getUserByEmail(userModel.getEmail());
                userfromSw360DB.setUserGroup(ThriftEnumUtils.stringToEnum(userGroup, UserGroup.class));
                userService.updateUser(userfromSw360DB);
            } catch (JsonProcessingException e) {
                log.info("CustomEventListenerSW360::onEvent(_,_)::Json processing error(GROUP)-->" + e);
            } catch (Exception e) {
                log.error("Error updatin the user while updating the user group", e);
            }
        } else if (OperationType.DELETE.equals(event.getOperationType())) {
            log.info("Group Details:::(Group Membership Event - DELETE)" + event.getRepresentation());
        }
    }

    private UserModel getUserModelFromSession(String resourcePath) {
        String userId = getUserIdfromResourcePath(resourcePath);
        RealmModel realm = keycloakSession.realms().getRealmByName(REALM);
        return keycloakSession.users().getUserById(realm, userId);
    }

    private String getUserIdfromResourcePath(String resourcePath) {
        int startIndex = resourcePath.indexOf("users/") + "users/".length();
        int endIndex = resourcePath.indexOf("/", startIndex);
        return resourcePath.substring(startIndex, endIndex);
    }

    private void actionUserOpration(AdminEvent event) {
        log.info("User Details:::(Action Event)" + event.getOperationType());
    }

    private void updateUserOperation(AdminEvent event) {
        log.info("User Details:::(Update Event)" + event.getRepresentation());
        try {
            UserEntity userEntity = objectMapper.readValue(event.getRepresentation(), UserEntity.class);
            User user = convertEntityToUserThriftObj(userEntity);
            log.info("Converted Entity::" + user);
            Optional<RequestStatus> rs;
            try {
                User existingUsrInCouchDB = userService.getUserByEmail(userEntity.getEmail());
                user.setId(existingUsrInCouchDB.getId());
                user.setRevision(existingUsrInCouchDB.getRevision());
                rs = Optional.ofNullable(userService.updateUser(user));
                rs.ifPresentOrElse((u) -> {
                    log.info("Update Status::" + u);
                }, () -> {
                    log.info("User not UPDATED may be as it returned null status!");
                });
            } catch (Exception e) {
                log.error("Something went wrong updating the user", e);
            }
        } catch (JsonMappingException e) {
            log.info("CustomEventListenerSW360::onEvent(_,_)::Json mapping error-->" + e);
        } catch (JsonProcessingException e) {
            log.info("CustomEventListenerSW360::onEvent(_,_)::Json processing error-->" + e);
        }
    }

    private void createUserOperation(AdminEvent event) {
        log.info("User Details:::(CREATE Event)" + event.getRepresentation());
        try {
            UserEntity userEntity = objectMapper.readValue(event.getRepresentation(), UserEntity.class);
            log.info("Converted Entity::" + convertEntityToUserThriftObj(userEntity));
            Optional<User> user = Optional.ofNullable(userService.addUser(convertEntityToUserThriftObj(userEntity)));
            user.ifPresentOrElse((u) -> {
                log.info("Saved User Couchdb Id::" + u.getId());
            }, () -> {
                log.info("User not saved may be as it returned null!");
            });
        } catch (JsonMappingException e) {
            log.info("CustomEventListenerSW360::onEvent(_,_)::Json mapping error-->" + e);
        } catch (JsonProcessingException e) {
            log.info("CustomEventListenerSW360::onEvent(_,_)::Json processing error-->" + e);
        }
    }

    private User convertEntityToUserThriftObj(UserEntity userEntity) {
        User user = new User();
        Map<String, List<String>> userAttributes = userEntity.getAttributes();
        Optional<List<String>> userGroups = Optional.ofNullable(userEntity.getGroups());
        log.info("User groups: " + userGroups.map(List::toString).orElse("[]"));
        List<String> departments = userAttributes.get(CUSTOM_ATTR_DEPARTMENT);
        String department = "DEPARTMENT";
        if (null != departments && !departments.isEmpty()) {
            department = departments.get(0);
        }
        String email = userEntity.getEmail();
        user.setEmail(email);
        userGroups.ifPresentOrElse((ug) -> {
            String groupName = ug.stream().findFirst().get().replaceFirst("/", "");
            user.setUserGroup(ThriftEnumUtils.stringToEnum(groupName, UserGroup.class));
        }, () -> {
            user.setUserGroup(ThriftEnumUtils.stringToEnum("USER", UserGroup.class));
        });
        user.setDepartment(department);
        user.setFullname(userEntity.getFirstName() + " " + userEntity.getLastName());
        user.setGivenname(userEntity.getFirstName());
        user.setLastname(userEntity.getLastName());
        return user;
    }

    public void close() {
        log.info("CustomEventListenerSW360:::close() called!");
    }

}
