package uk.ac.sanger.scgcf.jira.lims.validations

import spock.lang.Shared
import uk.ac.sanger.scgcf.jira.lims.configurations.ConfigReader
import uk.ac.sanger.scgcf.jira.lims.configurations.JiraLimsServices
import uk.ac.sanger.scgcf.jira.lims.exceptions.RestServiceException
import uk.ac.sanger.scgcf.jira.lims.utils.EnvVariableAccess
import uk.ac.sanger.scgcf.jira.lims.utils.RestService

import static groovyx.net.http.ContentType.JSON
import groovyx.net.http.Method
import spock.lang.Specification

/**
 * Created by ke4 on 05/10/16.
 */
class SequencescapeProjectNameValidatorSpec extends Specification {

    @Shared sequencescapeDetails

    def setupSpec() {
        EnvVariableAccess.metaClass.static.getJiraLimsConfigFilePath = { "./src/test/resources/jira_lims_config.json" }
        sequencescapeDetails = ConfigReader.getServiceDetails(JiraLimsServices.SEQUENCESCAPE)
    }

    def "find invalid project name in Sequencescape should return false"() {

        setup: "Create a mock RestService, its parameters and the mocked response"
        def restServiceStub = Stub(RestService)
        Map<?, ?> requestHeaders = [:]
        requestHeaders.put('X-SEQUENCESCAPE-CLIENT-ID', sequencescapeDetails['apiKey'])
        String servicePath = "${sequencescapeDetails['apiVersion']}/${sequencescapeDetails['searchProjectByName']}"
        String invalidProjectName = "invalid"
        def params = [
                         "search": [
                             "name": invalidProjectName
                         ]
                     ]

        def responseStatus = 404
        def responseJSON = [
                "general": [
                        "no resources found with that search criteria"
                ]
        ]
        def responseMap = [
                response: [status: responseStatus],
                reader: responseJSON
        ]

        restServiceStub.request(
                Method.POST, requestHeaders, JSON, servicePath, params) >> responseMap

        def validator = new SequencescapeValidator()
        validator.restService = restServiceStub

        expect: "Check if validation failed"
        validator.validateProjectName(invalidProjectName) == SequencescapeEntityState.NOT_EXISTS
    }

    def "find not active project name in Sequencescape should return true"() {

        setup: "Create a mock RestService, its parameters and the mocked response"
        def restServiceStub = Stub(RestService)
        Map<String, String> requestHeaders = [:]
        requestHeaders.put('X-SEQUENCESCAPE-CLIENT-ID', sequencescapeDetails['apiKey'])
        String servicePath = "${sequencescapeDetails['apiVersion']}/${sequencescapeDetails['searchProjectByName']}"
        String validProjectName = "100 cycle test"
        def params = [
                "search": [
                        "name": validProjectName
                ]
        ]

        def responseStatus = 301
        def responseJSON =
                [
                        "project": [
                                "created_at": "2009-03-02 10:41:12 +0000",
                                "updated_at": "2014-04-07 16:15:42 +0100",
                                "uuid": "0ff51cfa-1234-5678-1234-00144f2062b9",
                                "approved": true,
                                "budget_cost_centre": null,
                                "budget_division": "R+D",
                                "collaborators": null,
                                "cost_code": "S1234",
                                "external_funding_source": null,
                                "funding_comments": "test run",
                                "funding_model": null,
                                "name": "test",
                                "project_manager": "John Smith",
                                "state": "inactive"
                        ]
                ]
        def responseMap = [
                response: [status: responseStatus],
                reader: responseJSON
        ]

        restServiceStub.request(
                Method.POST, requestHeaders, JSON, servicePath, params) >> responseMap

        def validator = new SequencescapeValidator()
        validator.restService = restServiceStub

        expect: "Check if validation succeed"
        validator.validateProjectName(validProjectName) == SequencescapeEntityState.INACTIVE
    }

    def "find valid project name in Sequencescape should return true"() {

        setup: "Create a mock RestService, its parameters and the mocked response"
        def restServiceStub = Stub(RestService)
        Map<String, String> requestHeaders = [:]
        requestHeaders.put('X-SEQUENCESCAPE-CLIENT-ID', sequencescapeDetails['apiKey'])
        String servicePath = "${sequencescapeDetails['apiVersion']}/${sequencescapeDetails['searchProjectByName']}"
        String validProjectName = "100 cycle test"
        def params = [
                "search": [
                        "name": validProjectName
                ]
        ]

        def responseStatus = 301
        def responseJSON =
                [
                    "project": [
                        "created_at": "2009-03-02 10:41:12 +0000",
                        "updated_at": "2014-04-07 16:15:42 +0100",
                        "uuid": "0ff51cfa-1234-5678-1234-00144f2062b9",
                        "approved": true,
                        "budget_cost_centre": null,
                        "budget_division": "R+D",
                        "collaborators": null,
                        "cost_code": "S1234",
                        "external_funding_source": null,
                        "funding_comments": "test run",
                        "funding_model": null,
                        "name": "test",
                        "project_manager": "John Smith",
                        "state": "active"
                    ]
                ]
        def responseMap = [
                response: [status: responseStatus],
                reader: responseJSON
        ]

        restServiceStub.request(
                Method.POST, requestHeaders, JSON, servicePath, params) >> responseMap

        def validator = new SequencescapeValidator()
        validator.restService = restServiceStub

        expect: "Check if validation succeed"
        validator.validateProjectName(validProjectName) == SequencescapeEntityState.ACTIVE
    }

    def "when the remote service errors should throw exception"() {

        setup: "Create a mock RestService, its parameters and the mocked response"
        def restServiceStub = Stub(RestService)
        Map<String, String> requestHeaders = [:]
        requestHeaders.put('X-SEQUENCESCAPE-CLIENT-ID', sequencescapeDetails['apiKey'])
        String servicePath = "${sequencescapeDetails['apiVersion']}/${sequencescapeDetails['searchProjectByName']}"
        String someProjectName = "some project"
        def params = [
                "search": [
                        "name": someProjectName
                ]
        ]

        def responseStatus = 501
        def responseJSON = [
                "general": [
                        "requested action is not supported on this resource"
                ]
        ]
        def responseMap = [
                response: [status: responseStatus],
                reader: responseJSON
        ]
        def expected_error_message = "The request was not successful. The server responded with 501 code. The error message is: $responseJSON".toString()

        restServiceStub.request(
                Method.POST, requestHeaders, JSON, servicePath, params) >> responseMap

        def validator = new SequencescapeValidator()
        validator.restService = restServiceStub

        when: "unexpected condition was encountered"
        validator.validateProjectName(someProjectName)

        then:
        RestServiceException ex = thrown()
        def matcher = ex.message =~ /(?s).*$expected_error_message.*/
        assert matcher.matchesPartially()
    }
}
