import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.IssueManager
import com.atlassian.query.Query
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.slotegrator.rocketchat.RocketChatConnector



Collection<MutableIssue> searchIssue (String jqlSearch) {
    IssueManager issueManager = ComponentAccessor.getIssueManager()
    ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')
    def searchService = ComponentAccessor.getComponentOfType(SearchService.class)
    def parseResult = searchService.parseQuery(automationUser, jqlSearch)
    if (!parseResult.valid) {
        log.error("Invalid JQL: ${jqlSearch}")
        return
    }
    Object searchResults  = searchService.search(automationUser, parseResult.query, PagerFilter.unlimitedFilter)
    Collection issues = searchResults.getResults().collect { is -> issueManager.getIssueObject(is.getId()) }
    return issues
}

RocketChatConnector rocketChatConnector = new RocketChatConnector()
String rocketChatMessage = "Отсутствующие люди на следующей неделе:" + "\n"
rocketChatMessage += "Утвержденные "+ "\n"
CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
CustomField startDate = customFieldManager.getCustomFieldObject(11016L)
CustomField replacement = customFieldManager.getCustomFieldObject(11300L)
CustomField teamLead = customFieldManager.getCustomFieldObject(11011L)
String absentNextWeekApprovedJQL = "project in (TMHR, TMHRV2) AND 'Start Date' <= endOfDay('+1d') AND due >= endOfDay('+20d') AND status in (Approved, Feedback, Closed)"
Collection approvedIssuesAbsentNextWeekApproved = searchIssue (absentNextWeekApprovedJQL)
for (issue in approvedIssuesAbsentNextWeekApproved){
    String replacementValue = replacement.getValue(issue) ?replacement.getValue(issue).name: 'No replacement'
    rocketChatMessage += issue.reporter.name + " - " + issue.issueType.name + " [" + startDate.getValue(issue).format("yyyy.MM.dd") + "-" + issue.dueDate.format("yyyy.MM.dd") + "] - " +  replacementValue +"\n"
}
rocketChatMessage += "\n\n На утверждении" + "\n\n"
String absentTomorrowNotApprovedTLJQL = "project in (TMHR, TMHRV2) AND 'Start Date' <= endOfDay('+3d') AND due >= endOfDay('+7d') AND status = 'TL Approve'"
Collection notApprovedTLIssues = searchIssue (absentTomorrowNotApprovedTLJQL)
for (issue in notApprovedTLIssues){
    String teamLeadValue = teamLead.getValue(issue) ?teamLead.getValue(issue).name: 'No Team Lead'
    String replacementValue = replacement.getValue(issue) ?replacement.getValue(issue).name: 'No replacement'
    rocketChatMessage += issue.reporter.name + " - " + issue.issueType.name + " - " + replacementValue + " @" + teamLeadValue + " [" + issue.key  + "](https://jira.platform.live/browse/" + issue.key + ")\n"
}
String absentTomorrowNotApprovedPMJQL = "project in (TMHR, TMHRV2) AND 'Start Date' <= endOfDay('+3d') AND due >= endOfDay('+7d') AND status = 'PM Approve'"
Collection  notApprovedPMIssues = searchIssue (absentTomorrowNotApprovedPMJQL)
for (issue in notApprovedPMIssues){
    String teamLeadValue = teamLead.getValue(issue) ?teamLead.getValue(issue).name: 'No Team Lead'
    String replacementValue = replacement.getValue(issue) ?replacement.getValue(issue).name: 'No replacement'
    rocketChatMessage += issue.reporter.name + " - " + issue.issueType + " - " + replacementValue + " @" + teamLeadValue + " [" + issue.key  + "](https://jira.platform.live/servicedesk/customer/portal/2/" + issue.key + ")\n"
}
rocketChatMessage += "***" + "\n\n"
//rocketChatConnector.postMessage(rocketChatMessage, "##jira_vacations")

log.error ("Send message --> " + rocketChatMessage)


