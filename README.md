# session-service

RESTful API to cBioPortal/cbioportal sessions in MongoDB.  

Session information is stored in JSON, so this API generalizes to any JSON objects.

### Requirements

JDK 1.7 or later: http://www.oracle.com/technetwork/java/javase/downloads/index.html

Maven 3.0+: http://maven.apache.org/download.cgi

MongoDB: https://docs.mongodb.org/manual/

### Installation and setup

Create database 'session_service' using the 'mongo' shell interface to MongoDB:

```
$ mongo

> use session_service
```
Clone repository, compile, run tests, and start server:
```
$ git clone https://github.com/cBioPortal/session-service.git

$ cd session-service

session-service$ mvn package -Dpackaging.type=jar && java -jar target/session_service-0.1.0.jar
```

To run with a different profile (e.g. application-PROFILE.properties):

```
session-service$ mvn package -Dpackaging.type=jar && java -jar target/session_service-0.1.0.jar --spring.profiles.active=PROFILE
```

Generate war file:

```
$ cd session-service

session-service$ mvn package
```
To generate war file with a different profile (e.g. application-PROFILE.properties):
```
mvn package -Dspring.profiles.active=PROFILE
```

In production run with an external configuration file using a JVM property so that database properties are not saved in Github:

```
-Dspring.config.location=/srv/myapp/config.properties
```

To have a context root that is not "/", change the application-PROFILE.properties file:
```
server.contextPath=/session_service
server.port=8080
```

Assumptions: 

* When running 'local' profile (using application-local.properties) assumes MongoDB 
is running on localhost, default port 27017, that the database name is 'session_service' 
and that no username and password are required.
If any of these things are not true (hopefully the database is password protected)
run with an external file using a command line switch:
```
--spring.config.location=/srv/myapp/config.properties
```
Or setting a JVM property:
```
-Dspring.config.location=/srv/myapp/config.properties
```

* Assumes you want to run the server on port 8080.  This can be overridden by
setting the process's SERVER_PORT environment variable.
```
session-service$ export set SERVER_PORT=8090; mvn package -Dpackaging.type=jar && java -jar target/session_service-0.1.0.jar
```


## API

### Create

#### POST http://localhost:8080/api/sessions/{source}/{type}/
Creates a session.  Returns status 200 and the session id in response body
on success.  The session is saved in a collection named {type}. Both
source and type are saved in the session document. Valid types are:
main_session and virtual_cohort.  If a session with the same source, type,
and data already exists in the database returns the session id of that session
instead of creating a duplicate.  

WARNING: This is case sensitive. You should always use the same case
for source and type.

Example body for POST http://localhost:8080/api/sessions/msk_portal/main_session/
```
{"title": "my main portal session", "description": "this is an example"}
```
Example response:
```
{
  "id": "57167a52ef86d81afb415aba"
}
```
If no JSON data passed in request body or an invalid type is sent returns 400 status
with something like the following in the body:
```
{
  "timestamp": 1461093154793,
  "status": 400,
  "error": "Bad Request",
  "exception": "org.cbioportal.session_service.domain.exception.SessionInvalidException",
  "message": "valid types are: 'main_session' and 'virtual_cohort';",
  "path": "/api/sessions/msk_portal/invalid_type/"
}
```
Sending invalid JSON in the request body returns a 400 status
with something like the following in the body:
```
{
  "timestamp": 1461090997119,
  "status": 400,
  "error": "Bad Request",
  "exception": "org.cbioportal.session_service.domain.exception.SessionInvalidException",
  "message": "\n{\"portal-session\": blah blah blah}\n                   ^",
  "path": "/api/sessions/msk_portal/main_session/"
}
```

### Read

#### GET http://localhost:8080/api/sessions/{source}/{type}/
Returns all sessions for source and type.  Returns "[]" if no sessions.  
Example response for GET http://localhost:8080/api/sessions/msk_portal/main_session/
```
[
  {
    "id": "57167a52ef86d81afb415aba",
    "data": {
      "title": "my main portal session",
      "description": "this is an example"
    },
    "source": "msk_portal",
    "type": "main_session"
  },
  {
    "id": "57167c69ef86fdfcec850342",
    "data": {
      "title": "my main portal session",
      "description": "this is another example"
    },
    "source": "msk_portal",
    "type": "main_session"
  }
]
```

#### GET http://localhost:8080/api/sessions/{source}/{type}/{id}
Returns single session given source, type, and id.
Example response for GET http://localhost:8080/api/sessions/msk_portal/main_session/57167a52ef86d81afb415aba
```
{
  "id": "57167a52ef86d81afb415aba",
  "data": {
    "title": "my main portal session",
    "description": "this is an example"
  },
  "source": "msk_portal",
  "type": "main_session"
}
```
If no session is found returns status 404 with a request body like this:
```
{
  "timestamp": 1461091785628,
  "status": 404,
  "error": "Not Found",
  "exception": "org.cbioportal.session_service.web.SessionServiceController$SessionNotFoundException",
  "message": "Could not find session '0'.",
  "path": "/api/sessions/msk_portal/main_session/0"
}
```
WARNING: This is case sensitive.
GET http://localhost:8080/api/sessions/MSK_portal/main_session/57167a52ef86d81afb415aba
and
GET http://localhost:8080/api/sessions/msk_portal/Main_Session/57167a52ef86d81afb415aba 
are NOT equivalent.

#### GET http://localhost:8080/api/sessions/{source}/{type}/query?field={field}&value={value}
Returns all sessions matching a query for source and type. Returns
200 status on success.
Example response for GET http://localhost:8080/api/sessions/msk_portal/main_session/query?field=data.title&value=my%20main%20portal%20session
```
[
  {
    "id": "57167c69ef86fdfcec850342",
    "data": {
      "title": "my main portal session",
      "description": "this is another example"
    },
    "source": "msk_portal",
    "type": "main_session"
  }
]
```

### Update

#### PUT http://localhost:8080/api/sessions/{source}/{type}/{id}
Updates a session given the source, type, and id.  Returns status 200
on success with empty request body. 
Example body for PUT http://localhost:8080/api/sessions/msk_portal/main_session/57167a52ef86d81afb415aba
```
{
    "title": "my UPDATED main portal session",
    "description": "this is an example"
}
```
If no JSON data passed in request body returns status 400 with a request
body like this:
```
{
  "timestamp": 1461092375741,
  "status": 400,
  "error": "Bad Request",
  "exception": "org.springframework.http.converter.HttpMessageNotReadableException",
  "message": "Required request body is missing: public void org.cbioportal.session_service.web.SessionServiceController.updateSession(java.lang.String,java.lang.String,java.lang.String,java.lang.String)",
  "path": "/api/sessions/msk_portal/main_session/57167a52ef86d81afb415aba"
}
```
If an invalid id is passed returns status 404 with a request body like this:
```
{
  "timestamp": 1461092405701,
  "status": 400,
  "error": "Bad Request",
  "exception": "org.springframework.http.converter.HttpMessageNotReadableException",
  "message": "Required request body is missing: public void org.cbioportal.session_service.web.SessionServiceController.updateSession(java.lang.String,java.lang.String,java.lang.String,java.lang.String)",
  "path": "/api/sessions/msk_portal/main_session/0"
}
```
Sending invalid JSON in the request body returns a 400 status
with something like the following in the body:
```
{
  "timestamp": 1461092440979,
  "status": 400,
  "error": "Bad Request",
  "exception": "org.cbioportal.session_service.domain.exception.SessionInvalidException",
  "message": "\n{\n    \"title\": \"my UPDATED main portal session\",\n    \"description\": blah blah blah\n}\n                                                                    ^",
  "path": "/api/sessions/msk_portal/main_session/57167a52ef86d81afb415aba"
}
```

### Delete

#### DELETE http://localhost:8080/api/sessions/{source}/{type}/{id}
Deletes a session with source, type, and id.
Returns 200 status on success with empty request body. 
Example URL for DELETE http://localhost:8080/api/sessions/msk_portal/main_session/57167c69ef86fdfcec850342

If an invalid id is passed returns status 404 with a request body like this:
```
{
  "timestamp": 1461091373426,
  "status": 404,
  "error": "Not Found",
  "exception": "org.cbioportal.session_service.web.SessionServiceController$SessionNotFoundException",
  "message": "Could not find session '0'.",
  "path": "/api/sessions/msk_portal/main_session/0"
}
```
