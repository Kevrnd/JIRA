import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.IssueManager
import com.atlassian.query.Query
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.ServiceResultImpl
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.bc.issue.IssueService



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
IssueService issueService = ComponentAccessor.issueService
ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')
IssueLinkManager linkManager = ComponentAccessor.getIssueLinkManager()
Date date = new Date()
String currentDate = date.format("MM.yyyy")   // '03.2023'
String issueSummary = "BugFix RC " + currentDate
String jqlSearch1 = "Project = TW AND issuetype = Task AND summary ~ '" + issueSummary + "'"
// WARN!!!!!!!!!!!!    What the issue type is it???
String jqlSearch2 = "Project != 'TW' AND issuetype in (Bug, 'Internal issue', 'Production Bug', 'QA Task', Task, Tech-Task, Sub-task) AND resolution = Unresolved AND assignee in (a.vlassenko) ORDER BY updated DESC"
Collection searchResult1 = searchIssue(jqlSearch1)
Collection searchResult2 = searchIssue(jqlSearch2)
if (searchResult1.size() > 0){
    log.error ("Found TW monthly task [" + searchResult1 + "] Current linked issues: " + searchResult2)
    for (issue in searchResult2){
        if (issue != searchResult1[0]){
            linkManager.createIssueLink(searchResult1[0].id, issue.id, 10003L, 1L, automationUser)
        }
        log.error ("Finished updating the list of related tasks to [" + searchResult1[0] + "] Updated the list of related tasks: " + searchResult2)
    }

} else {
    String lastDayOfMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
    String dueDate = lastDayOfMonth + '/' + date.format("MMM/yy")
    String assigneUser = 'a.vlassenko'
    String issueDescription = "Ежемесячная задача." + "\n"
    issueDescription += "Доска для отслеживания задач в других проектах:" + "\n"
    issueDescription += "https://jira.platform.live/secure/Dashboard.jspa?selectPageId=12902" + "\n"
    issueDescription += "Будет отображать личные задачи в других проектах." + "\n"
    issueDescription += "Если на вас назначена эта задача - требуется открыть доску по ссылке выше и выполнить задачи из других проектов, которые назначены на вас." + "\n"
    issueDescription += "Время для Tempo можно списывать на эту задачу. В комментариях укажите ссылку на выполненную задачу в другом проекте." + "\n"
    def issueInputParameters = issueService.newIssueInputParameters().with {
        setAssigneeId(assigneUser)
        setComponentIds(13034L) //name='Other'
        setDescription(issueDescription)
        setDueDate(dueDate)
        setIssueTypeId('10002') //TASK
        setProjectId(10130L)
        setPriorityId('3') // Medium
        setReporterId(automationUser.name)
        setSummary(issueSummary)
    }
    def validationResult =issueService.validateCreate(automationUser, issueInputParameters)
    assert validationResult.valid : validationResult.errorCollection
    String newIssueSummary = validationResult.issue.getSummary()
    String jqlSearch3 = "summary ~ '${newIssueSummary}'"
    Collection searchResult3 = searchIssue(jqlSearch3)
    // check
    if  (!searchResult3) {
        def issueResult = issueService.create(automationUser, validationResult)
        assert issueResult.valid : issueResult.errorCollection
        log.error("Create ${issueResult.issue.getKey()}")
    }
}
