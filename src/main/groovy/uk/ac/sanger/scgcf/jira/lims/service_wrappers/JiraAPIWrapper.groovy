package uk.ac.sanger.scgcf.jira.lims.service_wrappers

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser
import groovy.util.logging.Slf4j
import uk.ac.sanger.scgcf.jira.lims.utils.WorkflowUtils

/**
 * This class handles interactions with the Jira API
 *
 * Created by as28 on 23/06/16.
 */

@Slf4j(value = "LOG")
class JiraAPIWrapper {

    static CustomFieldManager customFieldManager

    static CustomFieldManager getCustomFieldManager() {
        if (customFieldManager == null) {
            customFieldManager = ComponentAccessor.getCustomFieldManager()
        }
        customFieldManager
    }

    /**
     * Get a custom field object from its name
     *
     * @param cfName
     * @return CustomField object
     */
    static CustomField getCustomFieldByName(String cfName) {
        LOG.debug "Custom field name: ${cfName}"
        // assumption here that custom field name is unique
        def customFields = getCustomFieldManager().getCustomFieldObjectsByName(cfName)
        if (customFields != null) {
            customFields[0]
        } else {
            LOG.debug("No custom fields found with name: ${cfName}")
            null
        }
    }

    /**
     * Get the value of a specified custom field for an issue
     *
     * @param curIssue
     * @param cfName
     * @return String value of custom field
     * TODO: split out to handle custom fields other than simple strings
     * TODO: handle various exceptions and fail silently
     */
    static String getCFValueByName(Issue curIssue, String cfName) {
        LOG.debug "Custom field name: ${cfName}"
        String cfValue = curIssue.getCustomFieldValue(getCustomFieldByName(cfName)) as String
        LOG.debug("CF value: ${cfValue}")
        cfValue
    }

//    /**
//     * Useful method for retrieving multi-select custom field value as List<String>
//     */
//    @Nonnull
//    public List<String> getMultiSelectFieldValue(@Nonnull String fieldName, @Nonnull Issue issue) {
//        Validate.notNull(fieldName);
//        Validate.notNull(issue);
//        final CustomField customField = getCustomFieldByName(fieldName);
//
//        // Let's use the Option interface here as well instead of a specific implementation
//        @SuppressWarnings("unchecked")
//        final List<Option> value = (List<Option>) issue.getCustomFieldValue(customField);
//        // Handle NullPointerException
//        if (value == null) {
//            LOG.debug(
//                    "No value assigned to custom field '{}' on issue {}. Returning empty list.",
//                    customField, issue.getKey()
//            );
////            return Lists.newArrayList();
//            return new ArrayList<>()
//        }
//        // Handle non-list return values
//        if (!(value instanceof List)) {
//            LOG.debug(
//                    "Value of custom field '{}' on issue {} was not a List. Returning empty list.",
//                    customField, issue.getKey()
//            );
////            return Lists.newArrayList();
//            return new ArrayList<>()
//        }
//        // If it's empty, lets just return a new empty string list and forget about the origin type
//        if (value.isEmpty()) {
////            return Lists.newArrayList();
//            return new ArrayList<>()
//        }
//        // Handle potential ClassCastException for lists of any other kind, like Label
//        if (!(value.get(0) instanceof Option)) {
//            LOG.debug(
//                    "Value of custom field '{}' on issue {} was not a List<Option>. Returning empty list.",
//                    customField, issue.getKey()
//            );
////            return Lists.newArrayList();
//            return new ArrayList<>()
//        }
//        // Java 8
//        return value.stream()
//                .map(Option::getValue)
//                .collect(Collectors.toList());
//    }

    /**
     * Set the value of a specified custom field for an issue
     *
     * @param curIssue
     * @param cfName
     * @param newValue
     * TODO: this needs to handle custom fields other than strings
     */
    static void setCustomFieldValueByName(Issue curIssue, String cfName, String newValue) {
        LOG.debug "setCustomFieldValueByName: Custom field name: ${cfName}"
        LOG.debug "setCustomFieldValueByName: New value: ${newValue}"

        IssueService issueService = ComponentAccessor.getIssueService()

        // locate the custom field for the current issue from its name
        def tgtField = getCustomFieldManager().getCustomFieldObjects(curIssue).find { it.name == cfName }
        if (tgtField == null) {
            LOG.error "setCustomFieldValueByName: Custom field with name <${cfName}> was not found, cannot set value"
            //TODO: error handling
            return
        }

        // update the value of the field and save the change in the database
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters()
        LOG.debug "setCustomFieldValueByName: tgtField ID : ${tgtField.getId()}"

        issueInputParameters.addCustomFieldValue(tgtField.getId(), newValue)

        IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(WorkflowUtils.getLoggedInUser(), curIssue.getId(), issueInputParameters)

        if (updateValidationResult.isValid()) {
            LOG.debug "setCustomFieldValueByName: Issue update validated, running update"
            IssueService.IssueResult updateResult = issueService.update(WorkflowUtils.getLoggedInUser(), updateValidationResult);
            if (!updateResult.isValid()) {
                LOG.error "setCustomFieldValueByName: Custom field with name <${cfName}> could not be updated to value <${newValue}>"
                // TODO: error handling
            }
        } else {
            LOG.error "setCustomFieldValueByName: updateValidationResult false, custom field with name <${cfName}> could not be updated to value <${newValue}>"
            // TODO: error handling
        }
    }

    /**
     * Clear the value of a specified custom field for an issue
     *
     * @param cfName
     * TODO: this needs to handle custom fields other than strings
     */
    static void clearCustomFieldValueByName(Issue curIssue, String cfName) {
        setCustomFieldValueByName(curIssue, cfName, "")
    }

    /**
     * Get the id of a specified custom field for an issue
     *
     * @param curIssue current issue
     * @param cfName name of the custom field
     * @return String id of custom field
     */
    static String getCustomFieldIDByName(String cfName) {
        LOG.debug "Custom field name: ${cfName}"
        String cfID = getCustomFieldByName(cfName).id
        LOG.debug("CF idString: ${cfID}")
        cfID
    }

}