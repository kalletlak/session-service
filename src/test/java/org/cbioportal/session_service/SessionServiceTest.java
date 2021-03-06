/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

/*
 * This file is part of cBioPortal Session Service.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.cbioportal.session_service;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.net.URL;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;

/**
 * @author Manda Wilson 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SessionService.class)
@WebAppConfiguration
// pick random port for testing
@IntegrationTest({"server.port=0"})
// use application-test.properties config file
@ActiveProfiles("test")
public class SessionServiceTest {

    // get randomly assigned port
    @Value("${local.server.port}")
    private int port;

    private URL base;
    private RestTemplate template;

    @Before
    public void setUp() throws Exception {
        this.base = new URL("http://localhost:" + port + "/api/sessions/");
        template = new TestRestTemplate();
    }

    @After
    public void tearDown() throws Exception {
        // get all and delete them
        ResponseEntity<String> response = template.getForEntity(base.toString(), String.class);
        List<String> ids = parseIds(response.getBody());
        for (String id : ids) { 
			template.delete(base.toString() + id);
		}
    }

    @Test
    public void getSessionsNoData() throws Exception {
        ResponseEntity<String> response = template.getForEntity(base.toString(), String.class);
        assertThat(response.getBody(), equalTo("[]"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    public void getSessionsData() throws Exception {
        // first add data
        String data = "\"portal-session\":\"my session information\"";
        ResponseEntity<String> response = addData(data);

        // now test data is returned by GET /api/sessions/
        response = template.getForEntity(base.toString(), String.class);
        assertThat(expectedResponse(response.getBody(), data, true), equalTo(true)); 
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }
    
    @Test
    public void addSession() throws Exception {
        // add data
        String data = "\"portal-session\":\"my session information\"";
        ResponseEntity<String> response = addData(data);

        // test that we get the db record back and that the status was 200 
        assertThat(expectedResponse(response.getBody(), data), equalTo(true)); 
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    public void addSessionNoData() throws Exception {
        // add {} actually works TODO decide if it should
        String data = "";
        ResponseEntity<String> response = addData(data);

        // test that we get the db record back and that the status was 200 
        assertThat(expectedResponse(response.getBody(), data), equalTo(true)); 
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
       
        response = addData(null); 
        assertThat(response.getBody(), containsString("Required request body is missing"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void addSessionInvalidData() throws Exception {
        ResponseEntity<String> response = addData("\"portal-session\":blah blah blah"); 
        assertThat(response.getBody(), containsString("com.mongodb.util.JSONParseException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void getSession() throws Exception {
        // first add data
        String data = "\"portal-session\":{\"arg1\":\"first argument\"}";
        ResponseEntity<String> response = addData(data);

        // get id
        List<String> ids = parseIds(response.getBody());
        assertThat(ids.size(), equalTo(1));
        String id = ids.get(0);

        // now test data is returned by GET /api/sessions/[ID]
        response = template.getForEntity(base.toString() + id, String.class);
        System.out.println("getSession response body = " + response.getBody());
        assertThat(expectedResponse(response.getBody(), data), equalTo(true)); 
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    public void getSessionInvalidId() throws Exception {
        ResponseEntity<String> response = template.getForEntity(base.toString() + "id", String.class);
        System.out.println("getSession response body = " + response.getBody());
        assertThat(response.getBody(), containsString("org.cbioportal.session_service.web.SessionServiceController$SessionNotFoundException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    public void updateSession() throws Exception {
        String data = "\"portal-session\":\"my session information\"";
        ResponseEntity<String> response = addData(data);
        System.out.println("MEW: updateSession() response body = " + response.getBody());
        assertThat(expectedResponse(response.getBody(), data), equalTo(true)); 

        // get id
        List<String> ids = parseIds(response.getBody());
        assertThat(ids.size(), equalTo(1));
        String id = ids.get(0);

        data = "\"portal-session\":\"my session UPDATED information\"";
        HttpEntity<String> entity = prepareData(data);
        response = template.exchange(base.toString() + id, HttpMethod.PUT, entity, String.class);
        assertThat(expectedResponse(response.getBody(), data), equalTo(true)); 
        assertThat(response.getBody(), containsString("UPDATED"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    public void updateSessionInvalidData() throws Exception {
        String data = "\"portal-session\":{\"arg1\":\"first argument\"}";
        ResponseEntity<String> response = addData(data);

        // get id
        List<String> ids = parseIds(response.getBody());
        assertThat(ids.size(), equalTo(1));
        String id = ids.get(0);

        HttpEntity<String> entity = prepareData("\"portal-session\":blah blah blah");
        response = template.exchange(base.toString() + id, HttpMethod.PUT, entity, String.class);
        assertThat(response.getBody(), containsString("com.mongodb.util.JSONParseException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void updateSessionInvalidId() throws Exception {
        HttpEntity<String> entity = prepareData("\"portal-session\":\"my session information\"");
        ResponseEntity<String> response = template.exchange(base.toString() + "id", HttpMethod.PUT, entity, String.class);
        System.out.println("getSession response body = " + response.getBody());
        assertThat(response.getBody(), containsString("org.cbioportal.session_service.web.SessionServiceController$SessionNotFoundException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    public void updateSessionNoData() throws Exception {
        String data = "\"portal-session\":\"my session information\"";
        ResponseEntity<String> response = addData(data);

        // get id
        List<String> ids = parseIds(response.getBody());
        assertThat(ids.size(), equalTo(1));
        String id = ids.get(0);

        HttpEntity<String> entity = prepareData(null);
        response = template.exchange(base.toString() + id, HttpMethod.PUT, entity, String.class);

        assertThat(response.getBody(), containsString("Required request body is missing"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void deleteSession() throws Exception {
        // first add data
        String data = "\"portal-session\":{\"arg1\":\"first argument\"}";
        ResponseEntity<String> response = addData(data);
        assertThat(expectedResponse(response.getBody(), data), equalTo(true)); 

        // get id
        List<String> ids = parseIds(response.getBody());
        assertThat(ids.size(), equalTo(1));
        String id = ids.get(0);

        // delete
        response = template.exchange(base.toString() + id, HttpMethod.DELETE, null, String.class);
        assertThat(response.getBody(), equalTo(null)); 
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        // confirm record is gone
        response = template.getForEntity(base.toString() + id, String.class);
        System.out.println("getSession response body = " + response.getBody());
        assertThat(response.getBody(), containsString("org.cbioportal.session_service.web.SessionServiceController$SessionNotFoundException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    public void deleteSessionInvalidId() throws Exception {
        ResponseEntity<String> response = template.exchange(base.toString() + "id", HttpMethod.DELETE, null, String.class);
        System.out.println("getSession response body = " + response.getBody());
        assertThat(response.getBody(), containsString("org.cbioportal.session_service.web.SessionServiceController$SessionNotFoundException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }


    private HttpEntity<String> prepareData(String data) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (data != null) {
            data = "{" + data + "}";
        }
        return new HttpEntity<String>(data, headers);
    }

    private ResponseEntity<String> addData(String data) throws Exception {
        HttpEntity<String> entity = prepareData(data);
        return template.exchange(base.toString(), HttpMethod.POST, entity, String.class);
    }

    /*
     * plural is false.
     */
    private boolean expectedResponse(String responseBody, String data) throws Exception {
        return expectedResponse(responseBody, data, false);
    }

    private boolean expectedResponse(String responseBody, String data, boolean plural) throws Exception {
        // { and } are special characters in regexes, but also used in JSON so we need to escape them
        System.out.println("MEW: data = " + data);
        data = data.replaceAll("\\{", "\\\\{");
        data = data.replaceAll("\\}", "\\\\}");
        System.out.println("MEW: data = " + data);
        String pattern = "\\{\"id\":\"([^\"]+)\",\"data\":\\{" + data + "\\}\\}";
        if (plural) {
            pattern = "\\[" + pattern + "\\]";
        }
        pattern = "^" + pattern + "$";
        System.out.println("MEW: pattern = " + pattern);
        System.out.println("MEW: responseBody = " + responseBody);
        Pattern expectedResponsePattern = Pattern.compile(pattern);
        Matcher responseMatcher = expectedResponsePattern.matcher(responseBody);
        return responseMatcher.matches();
    }

    private List<String> parseIds(String json) throws Exception {
        Pattern idPattern = Pattern.compile("\"id\":\"([^\"]+)\"");
        Matcher idMatcher = idPattern.matcher(json);
        System.out.println("MEW: " + json);
        List<String> ids = new ArrayList<String>();
        while (idMatcher.find()) {
            System.out.println("MEW: " + idMatcher.group(1));
            ids.add(idMatcher.group(1));
        }
        return ids;
    }
};
