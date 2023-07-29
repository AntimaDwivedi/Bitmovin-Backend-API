## Bitmovin-Backend-API
This Backend server will be responsible to generate Lower and Higher resolutions of a particular video through Bitmovin integration.<br>
Steps to do local setup for Low/High Res file:<br>
Clone this respository : https://github.com/AntimaDwivedi/Bitmovin-Backend-API.git<br>

Do mvn clean , mvn run <br>
Run the main application <b>CmsScriptsApplication</b> <br>
And hit the curl "curl --location --request POST 'http://localhost:8080/videoGeneration/listOfCids/listOfCids'
--header 'Content-Type: application/json'
--data-raw '{ "cids": "1166035801", "task": "LOW_RES", "userEmail": "antimadwivedi28@gmail.com" }'" <br>

Health url to check the application is "UP" or "NOT" : http://localhost:8080/actuator/health/

