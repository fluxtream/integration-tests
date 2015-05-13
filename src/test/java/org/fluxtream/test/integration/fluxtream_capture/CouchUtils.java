package org.fluxtream.test.integration.fluxtream_capture;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import java.net.MalformedURLException;

import static org.junit.Assert.fail;

/**
 * Created by candide on 12/05/15.
 */
public class CouchUtils {


    static CouchDbConnector getCouchDbConnector(String user_login, String user_token, String dbName) {
        // connect to couchdb using the credentials that were just created
        HttpClient httpClient = null;
        try {
            httpClient = new StdHttpClient.Builder()
                    .url("http://localhost:5984")
                    .username(user_login)
                    .password(user_token)
                    .build();
        } catch (MalformedURLException e) {
            fail("Couldn't connect to CouchDB");
        }
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
        return dbInstance.createConnector(dbName, false);
    }


    static CouchDbInstance getCouchDbInstance(String user_login, String user_token) {
        // connect to couchdb using the credentials that were just created
        HttpClient httpClient = null;
        try {
            httpClient = new StdHttpClient.Builder()
                    .url("http://localhost:5984")
                    .username(user_login)
                    .password(user_token)
                    .build();
        } catch (MalformedURLException e) {
            fail("Couldn't connect to CouchDB");
        }
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
        return dbInstance;
    }

}
