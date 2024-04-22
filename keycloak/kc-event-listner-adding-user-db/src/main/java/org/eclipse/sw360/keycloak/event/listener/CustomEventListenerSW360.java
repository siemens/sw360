package org.eclipse.sw360.keycloak.event.listener;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.keycloak.userprofile.DefaultUserProfile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomEventListenerSW360 implements EventListenerProvider {

    private static final String REALM = "sw360";

    private static final String CUSTOM_ATTR_DEPARTMENT = "department";

    private static final Logger log = Logger.getLogger(CustomEventListenerSW360.class);
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
        if (EventType.REGISTER.equals(event.getType())) {
            userRegistrationEvent(event);
        }
        log.info("CustomEventListenerSW360:::Exiting onEvent(_)::");
    }

    private void userRegistrationEvent(Event event) {
        Map<String, String> details = event.getDetails();
        User user = new User();
        // Setting custom user attribute(department) from session
        keycloakSession.getAttributes().entrySet().forEach(x -> {
            Object up = x.getValue();
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

        user.setEmail(details.get("email"));
        user.setFullname(details.get("first_name") + " " + details.get("last_name"));
        user.setExternalid(details.get("username"));
        user.setGivenname(details.get("first_name"));
        user.setLastname(details.get("last_name"));
        userService.addUser(user);
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
