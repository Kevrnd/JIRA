import com.atlassian.jira.event.type.EventTypeManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.IssueManager
import com.atlassian.query.Query
import com.atlassian.jira.issue.fields.FieldManager
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.project.Project
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder



EventTypeManager eventTypeManager = ComponentAccessor.getEventTypeManager()
ApplicationUser userAutomation = ComponentAccessor.getUserManager().getUserByName("automation")
IssueManager issueManager = ComponentAccessor.getIssueManager()
CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
CustomField teamLead = customFieldManager.getCustomFieldObject(11011L)
CustomField projectManager = customFieldManager.getCustomFieldObject(10528L)
CustomField username = customFieldManager.getCustomFieldObject(11014L)
MutableIssue issue = (MutableIssue) event.issue
Object changeTeamLead = event?.getChangeLog()?.getRelated("ChildChangeItem")?.find {it.field == "Team Lead"}
Object changeProjectManager = event?.getChangeLog()?.getRelated("ChildChangeItem")?.find {it.field == "Project manager"}
if (changeTeamLead) {
    Object tmFieldValue = issue.getCustomFieldValue(teamLead)?:null
    Object usernameFieldValue = issue.getCustomFieldValue(username)?:null
    String jqlSearch = "project = TMHRV2 AND status != Closed AND reporter = ${usernameFieldValue}"
    SearchService searchService = ComponentAccessor.getComponentOfType(SearchService)
    Object parseResult = searchService.parseQuery(userAutomation, jqlSearch)
    if (!parseResult.valid) {
        log.error("Invalid JQL: ${jqlSearch}")
    return
    }
    Object searchResults  = searchService.search(userAutomation, parseResult.query, PagerFilter.unlimitedFilter)
    Collection issues = searchResults.getResults().collect { is -> issueManager.getIssueObject(is.getId()) }
    for (is in issues){
        Object tmValueAc = is.getCustomFieldValue(teamLead)
        if (!tmValueAc.equals(tmFieldValue)){
        ModifiedValue newValue = new ModifiedValue(is.getCustomFieldValue(teamLead), tmFieldValue )
        teamLead.updateValue(null, is, newValue, new DefaultIssueChangeHolder())
        log.error ("ISSUE ${is.key} CHANGE FIELD VALUE TEAM LEAD")


        }    
    }
} else if (changeProjectManager) {
    Object projectManagerFieldValue = issue.getCustomFieldValue(projectManager)?:null
    Object usernameFieldValue = issue.getCustomFieldValue(username)?:null
    String jqlSearch = "project = TMHRV2 AND status != Closed AND reporter = ${usernameFieldValue}"
    SearchService searchService = ComponentAccessor.getComponentOfType(SearchService)
    Object parseResult = searchService.parseQuery(userAutomation, jqlSearch)
    if (!parseResult.valid) {
        log.error("Invalid JQL: ${jqlSearch}")
    return    
    }
    Object searchResults  = searchService.search(userAutomation, parseResult.query, PagerFilter.unlimitedFilter)
    Collection  issues = searchResults.getResults().collect { is -> issueManager.getIssueObject(is.getId()) }
    for (is in issues){
        Object projectManagerAc = is.getCustomFieldValue(projectManager)
        if (!projectManagerAc.equals(projectManagerFieldValue)){  
            ModifiedValue newValue = new ModifiedValue(is.getCustomFieldValue(projectManager), projectManagerFieldValue )
            projectManager.updateValue(null, is, newValue, new DefaultIssueChangeHolder())
            log.error ("ISSUE ${is.key} CHANGE FIELD VALUE PROJECT MANAGER")
        }    
    }
}






