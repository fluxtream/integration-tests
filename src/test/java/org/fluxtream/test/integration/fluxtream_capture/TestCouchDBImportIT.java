package org.fluxtream.test.integration.fluxtream_capture;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ektorp.CouchDbConnector;
import org.ektorp.Revision;
import org.fluxtream.test.integration.config.CommonConfiguration;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by candide on 08/05/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {ITApplication.class, CommonConfiguration.class})
@WebIntegrationTest
public class TestCouchDBImportIT {


    // THIS HAS TO BE LOWERCASE THIS IS WRONG THIS IS WRONG THIS IS WRONG THIS IS WRONG THIS IS WRONG
    public static final String FLX_COUCHDB_IMPORT_TESTER_USERNAME = "couchdbimporttester";
    public final String FLX_COUCHDB_IMPORT_TESTER_PASSWORD = "testtest";

    @Autowired
    CommonConfiguration env;

    @PersistenceContext
    EntityManager em;

    @Autowired
    TestRestHelper restHelper;

    @Before
    public void setUp() {
        tearDownCouchUser();
        tearDownUser();
        ResponseEntity<String> userResponseEntity = createUser();
        assertTrue(userResponseEntity.getStatusCode().value() < 400);
        assertTrue(getUsernames().contains(FLX_COUCHDB_IMPORT_TESTER_USERNAME));
    }

    @After
    public void tearDown() {
//        tearDownCouchUser();
//        tearDownUser();
    }

    public void tearDownCouchUser() {
        try {
            String userId = String.format("org.couchdb.user:%s", TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME);
            CouchDbConnector users = CouchUtils.getCouchDbConnector(env.couchAdminUserUsername, env.couchAdminUserPassword, "_users");
            List<Revision> revisions = users.getRevisions(userId);
            for (Revision revision : revisions) {
                users.delete(userId, revision.getRev());
            }
        } catch (Throwable t) {t.printStackTrace();}

        try { CouchUtils.getCouchDbInstance(env.couchAdminUserUsername, env.couchAdminUserPassword).deleteDatabase(String.format("self_report_db_deleted_observations_%s", TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME)); } catch(Throwable t) {t.printStackTrace();}
        try { CouchUtils.getCouchDbInstance(env.couchAdminUserUsername, env.couchAdminUserPassword).deleteDatabase(String.format("self_report_db_deleted_topics_%s", TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME)); } catch(Throwable t) {t.printStackTrace();}
        try { CouchUtils.getCouchDbInstance(env.couchAdminUserUsername, env.couchAdminUserPassword).deleteDatabase(String.format("self_report_db_observations_%s", TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME)); } catch(Throwable t) {t.printStackTrace();}
        try { CouchUtils.getCouchDbInstance(env.couchAdminUserUsername, env.couchAdminUserPassword).deleteDatabase(String.format("self_report_db_topics_%s", TestCouchDBImportIT.FLX_COUCHDB_IMPORT_TESTER_USERNAME)); } catch(Throwable t) {t.printStackTrace();}
    }

    public void tearDownUser() {
        try { deleteUser(); } catch (Throwable t) {t.printStackTrace();}
    }

    @Test
    public void all() {
        // create the couchdb databases and make sure we get user credentials to access them
        ResponseEntity<String> initCouchDBResponseEntity = couchDBInit();
        assertTrue(initCouchDBResponseEntity.getStatusCode().value() < 400);
        String tokenJSONString = initCouchDBResponseEntity.getBody();
        JSONObject tokenJSON = JSONObject.fromObject(tokenJSONString);
        assertTrue(tokenJSON!=null);
        assertTrue(tokenJSON.has("user_login"));
        assertTrue(tokenJSON.has("user_token"));

        String user_login = tokenJSON.getString("user_login");
        String user_token = tokenJSON.getString("user_token");
        CouchDbConnector topicsDb = CouchUtils.getCouchDbConnector(user_login, user_token, String.format("self_report_db_%s_%s", "topics", user_login));

        // now create a couple topics
        CouchTopic hungerTopic = createTopic(topicsDb, "Hunger");
        CouchTopic angerTopic = createTopic(topicsDb, "Anger");

        // check that they have been created in couchDb
        List<String> topicDocIds = topicsDb.getAllDocIds();
        assertTrue(topicDocIds.size()==2);

        // create a few observations
        CouchDbConnector observationsDb = CouchUtils.getCouchDbConnector(user_login, user_token, String.format("self_report_db_%s_%s", "observations", user_login));
        createObservation(hungerTopic.getId(), 2, "2015-05-02T07:00:00.000Z", observationsDb, "Comment 1");
        createObservation(hungerTopic.getId(), 4, "2015-05-03T09:00:00.000Z", observationsDb, "Comment 2");
        createObservation(hungerTopic.getId(), 3, "2015-05-04T08:00:00.000Z", observationsDb, "Comment 3");
        createObservation(hungerTopic.getId(), 2, "2015-05-05T07:00:00.000Z", observationsDb, "Comment 4");
        createObservation(hungerTopic.getId(), 4, "2015-05-06T09:00:00.000Z", observationsDb, "Comment 5");
        createObservation(hungerTopic.getId(), 3, "2015-05-07T08:00:00.000Z", observationsDb, "Comment 6");

        // check that the have been created OK
        List<String> observationDocIds = observationsDb.getAllDocIds();
        assertTrue(observationDocIds.size() == 6);

        // trigger a FluxtreamCapture connector update
        triggerFluxtreamCaptureConnectorUpdate();

        // check the database for up to 10 seconds until the right number of topics, observations and channel mappings are present
        assertTrue(countRepeatedly("select count(*) from Facet_FluxtreamCaptureTopic WHERE guestId=(select max(id) from Guest)", 2, 10, 1000));
        assertTrue(countRepeatedly("select count(*) from Facet_FluxtreamCaptureObservation WHERE guestId=(select max(id) from Guest)", 6, 10, 1000));
        assertTrue(countRepeatedly("select count(*) from ChannelMapping WHERE guestId=(select max(id) from Guest)", 2, 10, 1000));

        JSONObject fluxtreamCaptureSource = getFluxtreamCaptureSourceFromSourcesList();
        // there should only be one source named "FluxtreamCapture"
        assertTrue(fluxtreamCaptureSource.getString("name").equals("FluxtreamCapture"));

        // there should be exactly two channels named "Hunger" and "Anger"
        checkChannelNames(fluxtreamCaptureSource, new String[]{"Hunger", "Anger"});

        // let's add a topic and rename an existing one
        CouchTopic elatedTopic = createTopic(topicsDb, "Elated");
        renameTopic(topicsDb, "Hunger", "Faim");

        // trigger a FluxtreamCapture connector update
        triggerFluxtreamCaptureConnectorUpdate();

        // check that we now have 3 topics named "Faim", "Anger" and "Elated"
        assertTrue(countRepeatedly("select count(*) from Facet_FluxtreamCaptureTopic WHERE guestId=(select max(id) from Guest)", 3, 10, 1000));
        fluxtreamCaptureSource = getFluxtreamCaptureSourceFromSourcesList();
        checkChannelNames(fluxtreamCaptureSource, new String[]{"Faim", "Anger", "Elated"});

        // create a few 'elated' observations
        createObservation(elatedTopic.getId(), 2, "2015-05-02T08:20:00.000Z", observationsDb, "Happy");
        createObservation(elatedTopic.getId(), 4, "2015-05-03T10:10:00.000Z", observationsDb, "So Happy");
        createObservation(elatedTopic.getId(), 3, "2015-05-04T09:40:00.000Z", observationsDb, "Oh Joy");

        // trigger a FluxtreamCapture connector update
        triggerFluxtreamCaptureConnectorUpdate();

        assertTrue(countRepeatedly("select count(*) from Facet_FluxtreamCaptureObservation WHERE guestId=(select max(id) from Guest)", 9, 10, 1000));
    }

    private JSONObject getFluxtreamCaptureSourceFromSourcesList() {
        ResponseEntity<String> sourcesList = getSourcesList();
        assertTrue(sourcesList.getStatusCode().value()<400);

        JSONObject sourcesListJSON = JSONObject.fromObject(sourcesList.getBody());
        JSONArray sources = sourcesListJSON.getJSONArray("sources");

        return sources.getJSONObject(0);
    }

    private void renameTopic(CouchDbConnector topicsDb, String previousName, String newName) {
        List<String> allTopicDocIds = topicsDb.getAllDocIds();
        for (String topicDocId : allTopicDocIds) {
            CouchTopic couchTopic = topicsDb.get(CouchTopic.class, topicDocId);
            if (couchTopic.getName().equals(previousName)) {
                couchTopic.setName(newName);
                topicsDb.update(couchTopic);
                break;
            }
        }
    }

    private void checkChannelNames(JSONObject fluxtreamCaptureSource, String[] expectedChannelNames) {
        JSONArray fluxtreamCaptureChannels = fluxtreamCaptureSource.getJSONArray("channels");
        nextExpectedChannelName: for (String expectedChannelName : expectedChannelNames) {
            for (int i=0; i<fluxtreamCaptureChannels.size(); i++) {
                JSONObject channel = fluxtreamCaptureChannels.getJSONObject(i);
                if (channel.getString("name").equals(expectedChannelName))
                    continue nextExpectedChannelName;
            }
            fail("Could not find channel named: " + expectedChannelName);
        }
    }

    private boolean countRepeatedly(String nativeCountQuery, int expectedCount, int remainingCalls, int delayInMillis) {
        System.out.print(String.format("expecting asynchronous result (%s)...", nativeCountQuery));
        Query nativeQuery = em.createNativeQuery(nativeCountQuery);
        int count = ((BigInteger) nativeQuery.getSingleResult()).intValue();
        System.out.print(" count is " + count);
        if (count!=expectedCount) {
            if (remainingCalls==0) return false;
            System.out.println(" -> retrying...");
            try {
                Thread.sleep(delayInMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return countRepeatedly(nativeCountQuery, expectedCount, remainingCalls-1, delayInMillis);
        }
        System.out.println(" as expected");
        return true;
    }

    private ResponseEntity<String> triggerFluxtreamCaptureConnectorUpdate() {
        RestTemplate restTemplate = restHelper.getRestTemplate();
        String couchInitURL = String.format("%sfluxtream_capture/notify", env.targetHomeBaseUrl);
        ResponseEntity<String> response = restTemplate.exchange(couchInitURL, HttpMethod.POST,
                new HttpEntity(restHelper.getBasicAuthHeader(FLX_COUCHDB_IMPORT_TESTER_USERNAME, FLX_COUCHDB_IMPORT_TESTER_PASSWORD)), String.class);
        return response;
    }

    private void createObservation(String topicId, int value, String creationTime, CouchDbConnector db, String comment) {
        CouchObservation observation = new CouchObservation();
        observation.setCreationTime(creationTime);
        observation.setCreationDate(creationTime.substring(0, 10));
        observation.setUpdateTime(creationTime);
        observation.setCreationDate(creationTime.substring(0, 10));
        observation.setTimezone("Europe/Berlin");
        observation.setComment(comment);
        observation.setTopicId(topicId);
        observation.setValue(String.valueOf(value));
        DateTime dateTime = ISODateTimeFormat.dateTime().parseDateTime(creationTime);
        observation.setObservationTime(ISODateTimeFormat.dateTimeNoMillis().print(dateTime));
        db.create(observation);
    }

    private CouchTopic createTopic(CouchDbConnector db, String topicName) {
        CouchTopic topic = new CouchTopic();
        topic.setName(topicName);
        long now = System.currentTimeMillis();
        topic.setCreationTime(ISODateTimeFormat.dateTimeNoMillis().print(now));
        topic.setUpdateTime(ISODateTimeFormat.dateTimeNoMillis().print(now));
        topic.setType("Numeric");
        topic.setDefaultValue("1");
        db.create(topic);
        return topic;
    }

    private ResponseEntity<String> couchDBInit() {
        RestTemplate restTemplate = restHelper.getRestTemplate();
        String couchInitURL = String.format("%sapi/v1/couch/", env.targetHomeBaseUrl);
        ResponseEntity<String> response = restTemplate.exchange(couchInitURL, HttpMethod.PUT,
                new HttpEntity(restHelper.getBasicAuthHeader(FLX_COUCHDB_IMPORT_TESTER_USERNAME, FLX_COUCHDB_IMPORT_TESTER_PASSWORD)), String.class);
        return response;
    }

    private List getUsernames() {
        Query nativeQuery = em.createNativeQuery("SELECT username FROM Guest");
        return nativeQuery.getResultList();
    }

    public ResponseEntity<String>  createUser() throws RestClientException {
        RestTemplate restTemplate = restHelper.getRestTemplate();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
        body.add("username", FLX_COUCHDB_IMPORT_TESTER_USERNAME);
        body.add("firstname", "CouchDBImport");
        body.add("lastname", "Tester");
        body.add("password", FLX_COUCHDB_IMPORT_TESTER_PASSWORD);
        body.add("email", "couchDBImport@testers.com");

        HttpEntity<?> httpEntity = new HttpEntity<Object>(body, restHelper.getBasicAuthHeader(env.flxAdminUserUsername, env.flxAdminUserPassword));
        ResponseEntity<String> response = restTemplate.exchange(String.format("%sapi/v1/admin/create", env.targetHomeBaseUrl), HttpMethod.POST, httpEntity, String.class);

        return response;
    }

    public ResponseEntity<String> deleteUser() {
        RestTemplate restTemplate = restHelper.getRestTemplate();
        String deleteURL = String.format("%sapi/v1/admin/%s", env.targetHomeBaseUrl, FLX_COUCHDB_IMPORT_TESTER_USERNAME);
        ResponseEntity<String> response = restTemplate.exchange(deleteURL, HttpMethod.DELETE,
                new HttpEntity(restHelper.getBasicAuthHeader(env.flxAdminUserUsername, env.flxAdminUserPassword)), String.class);
        return response;
    }

    public ResponseEntity<String> getSourcesList() {
        RestTemplate restTemplate = restHelper.getRestTemplate();
        long guestId = getGuestId(FLX_COUCHDB_IMPORT_TESTER_USERNAME);
        String sourcesListURL = String.format("%sapi/v1/bodytrack/users/%s/sources/list", env.targetHomeBaseUrl, guestId);
        ResponseEntity<String> response = restTemplate.exchange(sourcesListURL, HttpMethod.GET,
                new HttpEntity(restHelper.getBasicAuthHeader(env.flxAdminUserUsername, env.flxAdminUserPassword)), String.class);
        return response;
    }

    private long getGuestId(String flxCouchdbImportTesterUsername) {
        Query nativeQuery = em.createNativeQuery("SELECT id FROM Guest WHERE username=?");
        nativeQuery.setParameter(1, flxCouchdbImportTesterUsername);
        Long guestId = ((BigInteger) nativeQuery.getSingleResult()).longValue();
        return guestId;
    }

}
