package org.fluxtream.test.integration.fluxtream_capture;

import org.ektorp.CouchDbConnector;
import org.ektorp.Revision;
import org.fluxtream.test.integration.config.CommonConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * Created by candide on 12/05/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {CommonConfiguration.class, ITApplication.class })
public class Utils {

    @Autowired
    CommonConfiguration env;

    @Autowired
    TestRestHelper restHelper;

//    @Test
//    public void deleteTestUser() {
//        RestTemplate restTemplate = restHelper.getRestTemplate();
//        String deleteURL = String.format("%sapi/v1/admin/%s", env.targetHomeBaseUrl, TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME);
//        ResponseEntity<String> response = restTemplate.exchange(deleteURL, HttpMethod.DELETE,
//                new HttpEntity(restHelper.getBasicAuthHeader(env.flxAdminUserUsername, env.flxAdminUserPassword)), String.class);
//    }

    @Test
    public void deleteTestCouchUser() {
        String userId = String.format("org.couchdb.user:%s", TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME);
        CouchDbConnector users = CouchUtils.getCouchDbConnector(env.couchAdminUserUsername, env.couchAdminUserPassword, "_users");
        List<Revision> revisions = users.getRevisions(userId);
        for (Revision revision : revisions) {
            users.delete(userId, revision.getRev());
        }

        CouchUtils.getCouchDbInstance(env.couchAdminUserUsername, env.couchAdminUserPassword).deleteDatabase(String.format("self_report_db_deleted_observations_%s", TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME));
        CouchUtils.getCouchDbInstance(env.couchAdminUserUsername, env.couchAdminUserPassword).deleteDatabase(String.format("self_report_db_deleted_topics_%s", TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME));
        CouchUtils.getCouchDbInstance(env.couchAdminUserUsername, env.couchAdminUserPassword).deleteDatabase(String.format("self_report_db_observations_%s", TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME));
        CouchUtils.getCouchDbInstance(env.couchAdminUserUsername, env.couchAdminUserPassword).deleteDatabase(String.format("self_report_db_topics_%s", TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME));
    }

}
