package org.sunbird.cassandraimpl;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.BaseTest;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionManagerImpl;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.helper.ServiceFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraConnectionMngrFactory.class, CassandraConnectionManagerImpl.class})
@PowerMockIgnore({"javax.management.*"})
public class CassandraDACImplTest extends BaseTest {

    static final String keyspace = "sunbird_courses";
    static final String table = "assessment_aggregator";
    static final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    @Mock
    CassandraConnectionManager connectionManager;

    private static String createKeyspace = "CREATE KEYSPACE IF NOT EXISTS " + keyspace
            + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}";
    private static String createTable = "CREATE TABLE IF NOT EXISTS " + keyspace + "." + table
            + " (course_id text,batch_id text,user_id text,content_id text,attempt_id text,created_on timestamp,grand_total text,last_attempted_on timestamp,total_max_score double,total_score double,updated_on timestamp,PRIMARY KEY (course_id, batch_id, user_id, content_id, attempt_id));";
    private static String insertTable = "INSERT INTO " + keyspace + "." + table
            + "(user_id, course_id, batch_id, content_id, attempt_id, total_max_score, total_score, last_attempted_on) VALUES ('user_001','course_001','batch_001', 'content_001', 'attempt_001', 1, 1, '" + timestamp + "');";

    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        executeScript(createKeyspace, createTable, insertTable);
    }

    @Test
    public void testGetRecordsWithLimit() {
        Request request = getRequest();
        Map<String, Object> filters = new HashMap<String, Object>() {
            {
                put("user_id", "user_001");
                put("course_id", "course_001");
                put("batch_id", "batch_001");
                put("content_id", new ArrayList<String>() {{
                    add("content_001");
                }});
            }
        };
        ArrayList<String> fieldsToGet = new java.util.ArrayList<String>() {{
            add("attempt_id");
            add("last_attempted_on");
            add("total_max_score");
            add("total_score");
        }};

        Map<String, Object> result = new HashMap<String, Object>() {{
            put("totalMaxScore", 1.0);
            put("lastAttemptedOn", timestamp);
            put("totalScore", 1.0);
            put("attemptId", "attempt_001");
        }};

        PowerMockito.stub(PowerMockito.method(CassandraConnectionMngrFactory.class, "getInstance")).toReturn(connectionManager);
        PowerMockito.stub(PowerMockito.method(CassandraConnectionManagerImpl.class, "getSession")).toReturn(session);
        Response response = cassandraOperation.getRecordsWithLimit(request.getRequestContext(), keyspace, table, filters, fieldsToGet, 25);
        Assert.assertEquals(response.getResponseCode(), ResponseCode.OK);
        Assert.assertTrue(((ArrayList<Map<String, Object>>) response.getResult().get("response")).get(0).equals(result));
    }

    public Request getRequest() {
        Request request = new Request();
        request.setContext(new HashMap<String, Object>() {
        });
        return request;
    }
}
