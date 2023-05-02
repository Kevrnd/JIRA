import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.IssueManager
import com.atlassian.query.Query
import com.atlassian.jira.component.ComponentAccessor
import com.slotegrator.rocketchat.RocketChatConnector
import java.text.SimpleDateFormat



IssueManager issueManager = ComponentAccessor.getIssueManager()
CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
CustomField birthday = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12370L)
CustomField jobPosition = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11015L)
CustomField hr = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12353)
ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')
String jqlSearch = "project = AC AND status = 'In Progress'  AND Birthday is not EMPTY ORDER BY cf[12370] ASC"
RocketChatConnector rocketChatConnector = new RocketChatConnector()
def searchService = ComponentAccessor.getComponentOfType(SearchService)
def parseResult = searchService.parseQuery(automationUser, jqlSearch)
if (!parseResult.valid) {
    log.error("Invalid JQL: ${jqlSearch}")
    return
}
Object searchResults  = searchService.search(automationUser, parseResult.query, PagerFilter.unlimitedFilter)
Collection issues = searchResults.getResults().collect { is -> issueManager.getIssueObject(is.getId()) }
boolean checkSendMessage = false
for (issue in issues){    
    SimpleDateFormat monthDate = new SimpleDateFormat('MMM')
    SimpleDateFormat dayDate = new SimpleDateFormat('dd')
    SimpleDateFormat weekDaysDate = new SimpleDateFormat ('EEE')
    SimpleDateFormat birthdayFormat = new SimpleDateFormat("dd MMM yyyy")
    Date today = new Date()
    Date tomorrow = today + 1
    Date afterTomorrow = today + 2
    Date afterAfterTomorrow = today + 3
    def birthdayInCard = birthday.getValue(issue)    
    String jobPositionInCard = jobPosition.getValue(issue)
    String hrInCard = hr.getValue(issue)
    String noticeHr = ''
    if (hrInCard == 'Platform'){
        noticeHr = '@d.merdenova '
    } else if (hrInCard == 'Slot'){
        noticeHr = '@a.dzhuhan '
    }
    def monthBirthdayInCard = monthDate.format(birthdayInCard)
    def dayBirthdayInCard = dayDate.format(birthdayInCard)

    if (weekDaysDate.format(today) == 'Fri') {
        if ((dayDate.format(tomorrow)  == dayBirthdayInCard) && (monthDate.format(tomorrow) == monthBirthdayInCard) ) {
            log.warn ("SEND MESSAGE-->"+ noticeHr + birthdayFormat.format(birthdayInCard) + " - " + issue.summary + " - " + jobPositionInCard)
            checkSendMessage  = true
            rocketChatConnector.postMessage(noticeHr + birthdayFormat.format(birthdayInCard) + " - " + issue.summary + " - " + jobPositionInCard, "#Jira_Birthday")
        } else if ((dayDate.format(afterTomorrow)  == dayBirthdayInCard) && (monthDate.format(afterTomorrow) == monthBirthdayInCard)){
            log.warn ("SEND MESSAGE-->"+ noticeHr + birthdayFormat.format(birthdayInCard) + " - " + issue.summary + " - " + jobPositionInCard)
            checkSendMessage  = true
            rocketChatConnector.postMessage(noticeHr + birthdayFormat.format(birthdayInCard) + " - " + issue.summary + " - " + jobPositionInCard, "#Jira_Birthday")
        } else if ((dayDate.format(afterAfterTomorrow)  == dayBirthdayInCard) && (monthDate.format(afterAfterTomorrow) == monthBirthdayInCard)) {
            log.warn ("SEND MESSAGE-->"+ noticeHr + birthdayFormat.format(birthdayInCard) + " - " + issue.summary + " - " + jobPositionInCard)
            checkSendMessage  = true
            rocketChatConnector.postMessage(noticeHr + birthdayFormat.format(birthdayInCard) + " - " + issue.summary + " - " + jobPositionInCard, "#Jira_Birthday")
        }
    } else {
        if ((dayDate.format(tomorrow)  == dayBirthdayInCard ) && (monthDate.format(tomorrow) == monthBirthdayInCard)) {
            log.warn ("SEND MESSAGE-->"+ noticeHr + birthdayFormat.format(birthdayInCard) + " - " + issue.summary + " - " + jobPositionInCard)
            checkSendMessage  = true
            rocketChatConnector.postMessage(noticeHr + birthdayFormat.format(birthdayInCard) + " - " + issue.summary + " - " + jobPositionInCard, "#Jira_Birthday")
        }

    }

}    
if (!checkSendMessage) {
    rocketChatConnector.postMessage('Завтра именинников нет', "#Jira_Birthday") //testNotify
    log.warn ("SEND MESSAGE--> Завтра именинников нет")
}
