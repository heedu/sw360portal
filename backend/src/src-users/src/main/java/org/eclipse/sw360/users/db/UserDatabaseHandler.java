/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.users.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.UserRepository;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftValidate;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.mail.MailConstants;
import org.eclipse.sw360.mail.MailUtil;
import org.ektorp.http.HttpClient;

import java.net.MalformedURLException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

/**
 * Class for accessing the CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 */
public class UserDatabaseHandler {

    /**
     * Connection to the couchDB database
     */
    private DatabaseConnector db;
    private UserRepository repository;

    public UserDatabaseHandler(Supplier<HttpClient> httpClient, String dbName) throws MalformedURLException {
        // Create the connector
        db = new DatabaseConnector(httpClient, dbName);
        repository =  new UserRepository(db);
    }

    public User getByEmail(String email) {
        return db.get(User.class, email);
    }

    private void prepareUser(User user) throws SW360Exception {
        // Prepare component for database
        ThriftValidate.prepareUser(user);
    }

    public RequestStatus addUser(User user) throws SW360Exception {
        prepareUser(user);
        // Add to database
        db.add(user);

        return RequestStatus.SUCCESS;
    }

    public RequestStatus updateUser(User user) throws SW360Exception {
        prepareUser(user);
        db.update(user);

        return RequestStatus.SUCCESS;
    }

    public RequestStatus deleteUser(User user, User adminUser) {
        if (makePermission(user, adminUser).isActionAllowed(RequestedAction.DELETE)) {
            repository.remove(user);
            return RequestStatus.SUCCESS;
        }
        return RequestStatus.FAILURE;
    }

    public RequestStatus sendMailForAcceptedModerationRequest(String userEmail) {

        MailUtil mailUtil = new MailUtil();
        mailUtil.sendMail(userEmail, MailConstants.SUBJECT_FOR_ACCEPTED_MODERATION_REQUEST, MailConstants.TEXT_FOR_ACCEPTED_MODERATION_REQUEST, true);

        return RequestStatus.SUCCESS;
    }

    public List<User> getAll() {return repository.getAll();}

    public List<User> searchUsers(String name) {
        return repository.searchByName(name);
    }
}
