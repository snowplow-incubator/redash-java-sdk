/*
 * Copyright (c) 2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.redash;

import com.snowplowanalytics.redash.model.Group;
import com.snowplowanalytics.redash.model.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Tests should be performed with only those user groups that are created by Redash server
 * right after installation and registration process have been completed.
 *
 * The included wipeDataSources() method will drop all user groups from Redash server
 * except "admin" and "default".
 */
public class RedashClientUserAndGroupTest extends AbstractRedashClientTest {

    @Before
    public void setup() throws IOException {
        wipeAllCreatedUserGroups();
    }

    @Test
    public void successfulCreateUserGroupTest() throws IOException {
        List<Group> groups = redashClient.getUserGroups();
        Group created = new Group("testGroup");
        Assert.assertTrue(groups.size() == 2);
        int id = redashClient.createUserGroup(created);
        groups = redashClient.getUserGroups();
        Assert.assertTrue(groups.size() == 3);
        Assert.assertTrue(groups.contains(created));
        redashClient.deleteUserGroup(id);
    }

    @Test(expected = IOException.class)
    public void getUserGroupsTest() throws IOException {
        wrongClient.getUserGroups();
    }

    @Test
    public void deleteUserGroupTest() throws IOException {
        Group created = new Group("name");
        int id = redashClient.createUserGroup(created);
        Assert.assertTrue(redashClient.getUserGroups().size() == 3);
        Assert.assertFalse(redashClient.deleteUserGroup(id + 1));
        Assert.assertTrue(redashClient.getUserGroups().size() == 3);
        Assert.assertTrue(redashClient.deleteUserGroup(id));
        Assert.assertTrue(redashClient.getUserGroups().size() == 2);
        id = redashClient.createUserGroup(created);
        Assert.assertTrue(redashClient.getUserGroups().size() == 3);
        try {
            wrongClient.deleteUserGroup(id);
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IOException.class));
        }
    }

    @Test
    public void unsuccessfulWithExistingNameCreateUserGroupTest() throws IOException {
        List<Group> groups = redashClient.getUserGroups();
        Assert.assertTrue(groups.size() == 2);
        try {
            redashClient.createUserGroup(new Group(defaultGroup.getName()));
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IllegalArgumentException.class));
        }
        groups = redashClient.getUserGroups();
        Assert.assertTrue(groups.size() == 2);
    }

    @Test
    public void unsuccessfulWithWrongClientCreateUserGroupTest() throws IOException {
        List<Group> groups = redashClient.getUserGroups();
        Assert.assertTrue(groups.size() == 2);
        try {
            wrongClient.createUserGroup(new Group(defaultUser.getName()));
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IOException.class));
        }
        groups = redashClient.getUserGroups();
        Assert.assertTrue(groups.size() == 2);
    }

    @Test
    public void getUsersTest() throws IOException {
        List<User> users = redashClient.getUsers();
        Assert.assertTrue(users.size() == 2);
        Assert.assertTrue(users.get(0).equals(adminUser));
        Assert.assertTrue(users.get(1).equals(defaultUser));
    }

    @Test(expected = IOException.class)
    public void getUsersWithIOExceptionTest() throws IOException {
        wrongClient.getUsers();
    }

    @Test
    public void getUserTest() throws IOException {
        User user = redashClient.getUser(adminUser.getName());
        Assert.assertTrue(user.equals(adminUser));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsuccessfulGetUserTest() throws IOException {
        redashClient.getUser(invalidUserName);
    }

    @Test(expected = IOException.class)
    public void getUserWithIOExceptionTest() throws IOException {
        wrongClient.getUser(defaultUser.getName());
    }

    @Test
    public void addUserToUserGroupTest() throws IOException {
        Group createdGroup = new Group("createdForTest");
        int createdUserGroupId = redashClient.createUserGroup(createdGroup);
        Group groupFromDb = redashClient.getWithUsersAndDataSources(createdUserGroupId);
        User userFromDb = redashClient.getUser(defaultUser.getName());
        Assert.assertFalse(groupFromDb.getUsers().contains(userFromDb));
        Assert.assertTrue(redashClient.addUserToGroup(userFromDb.getId(), createdUserGroupId));
        groupFromDb = redashClient.getWithUsersAndDataSources(createdUserGroupId);
        Assert.assertTrue(groupFromDb.getUsers().size() == 1);
        Assert.assertTrue(groupFromDb.getUsers().contains(userFromDb));
        Assert.assertFalse(redashClient.addUserToGroup(userFromDb.getId(), createdUserGroupId));
        groupFromDb = redashClient.getWithUsersAndDataSources(createdUserGroupId);
        Assert.assertTrue(groupFromDb.getUsers().size() == 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNonExistingUserToUserGroup() throws IOException {
        redashClient.addUserToGroup(4, defaultGroup.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUserToNonExistingUserGroup() throws IOException {
        redashClient.addUserToGroup(defaultUser.getId(), 3);
    }

    @Test(expected = IOException.class)
    public void addUserToUserGroupTestWithWrongKey() throws IOException {
        wrongClient.addUserToGroup(defaultUser.getId(), defaultGroup.getId());
    }

    @Test
    public void removeUserFromGroupTest() throws IOException {
        Group groupFromDb = redashClient.getWithUsersAndDataSources(defaultGroup.getId());
        Assert.assertTrue(groupFromDb.getUsers().size()==2);
        Assert.assertTrue(redashClient.removeUserFromGroup(defaultUser.getId(), defaultGroup.getId()));
        groupFromDb = redashClient.getWithUsersAndDataSources(defaultGroup.getId());
        Assert.assertTrue(groupFromDb.getUsers().size()==1);
        Assert.assertTrue(!groupFromDb.getUsers().contains(defaultUser));
        redashClient.addUserToGroup(defaultUser.getId(), defaultGroup.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNonExistingUserFromGroupTest() throws IOException {
        redashClient.removeUserFromGroup(defaultUser.getId() + 1, defaultGroup.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeUserFromNonExistingGroupTest() throws IOException {
        redashClient.removeUserFromGroup(defaultUser.getId(), defaultGroup.getId() + 1);
    }

    @Test(expected = IOException.class)
    public void removeUserGroupWithWrongClientTest() throws IOException {
        wrongClient.removeUserFromGroup(defaultUser.getId(), defaultGroup.getId());
    }

    // Helpers

    private void wipeAllCreatedUserGroups() throws IOException {
        redashClient.getUserGroups().forEach(userGroup -> {
            int id = userGroup.getId();
            try {
                if (id != 1 && id != 2) {
                    redashClient.deleteUserGroup(id);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
