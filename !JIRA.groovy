
import com.atlassian.jira.issue.issuetype.IssueType
import com.atlassian.jira.bc.project.component.ProjectComponent
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.config.ConstantsManager
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.IssueManager
import com.atlassian.query.Query
import com.atlassian.jira.issue.fields.FieldManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.project.Project
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption

//Дать Jira задачу в консоль
IssueManager issueManager = ComponentAccessor.getIssueManager()
MutableIssue issue = issueManager.getIssueObject("")

//user Automation
ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')

//Получиьть имя User по ID
import com.atlassian.jira.component.ComponentAccessor
ComponentAccessor.getUserManager().getUserById(13115)

//Тип задачи
issue.issueType.name == 'Bug'

//Приоритет задачи
issue.priority.name == "Blocker"||"Critical"

// Статус задачи
issue.getStatus().name

//Поле Custom Field
CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
CustomField r = customFieldManager.getCustomFieldObject(11014L)
return r.getValue(issue)

// Поле Components
Collection<ProjectComponent> listComponent = Issue.getComponents()

// Поле Affects Version/s:
def affectedVersion = issue.getAffectedVersions() //.first()

// Поле Fix Version
def fixVersion = issue.getFixVersions()//.first()

// Object IssueSummary = issue.getSummary()
Object IssueSummary = issue.getSummary()

//SET FIELD OPTION
if (selectedProduct == "Ford") {
getFieldById(AFFECTED_VERSIONS).setFieldOptions(["Figo","Focus"])
getFieldById(COMPONENTS).setFieldOptions(["Двигатель", "Коробка передач"])

}

//EVENT
// https://community.atlassian.com/t5/Jira-questions/Update-Issue-Listener-check-if-something-changed/qaq-p/1063661
def change = event?.getChangeLog()?.getRelated("ChildChangeItem")?.find {it.field == "priority"}


//Категория проекта для задачи
if  (issue.getProjectObject().getProjectCategory().getId() == 10300) {
    return true
}
//Корявый путь
long issueProjectId = issue.getProjectObject().id
def projectManager = ComponentAccessor.getProjectManager()
Object pojectListCategoryDeliveryProject20 = projectManager.getProjectObjectsFromProjectCategory(10300).id
if (!(issueProjectId in pojectListCategoryDeliveryProject20)){
    return null
}

//Добавить коммент
ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')
String commentBody = """ [~${assigneeIssue.name}] Вы являетесь исполнителем в задаче ${issue.key}. Задача находится в стаусе Review более 7 дней."""
commentManager.create(issue, automationUser,commentBody,true)


//Get appropriate managers/handlers
def commentManager = ComponentAccessor.getCommentManager()
def authContext = ComponentAccessor.getJiraAuthenticationContext()

//Define currentUser because it is not an available default
def authContext = ComponentAccessor.getJiraAuthenticationContext()
def currentUser = authContext.getLoggedInUser()

//Add comment to issue transitioned. The name on the comment will be the current user's name.
//The boolean value delegates whether or not an event is fired.
commentManager.create(issue, currentUser, "This is where the comment goes!", true)


// Посмотреть EVent name/id e.tc
def eventTypeManager = ComponentAccessor.getEventTypeManager()
def eventTypeName = eventTypeManager.getEventType(event.eventTypeId) // .getName() -- > getId()

log.error("EVENT TYPE NAME --> "+ eventTypeName)

// event Изменение поля Affect Version/s
def changeAffectVersion = event?.getChangeLog()?.
        getRelated("ChildChangeItem")?.find {it.field.toString() == "Version"}
log.error("EVENT TYPE NAME --> "+ changeAffectVersion)

//event  изменение поля приоритет задачи (поле приоритет)
MutableIssue issue = (MutableIssue) event.issue;
EventTypeManager eventTypeManager = ComponentAccessor.getEventTypeManager()
Comparable<Long> createId = eventTypeManager.getEventType(event.eventTypeId).id
def changePriority = event?.getChangeLog()?.
        getRelated("ChildChangeItem")?.find {it.field == "priority"}
log.error("EVENT TYPE NAME --> "+ changePriority)

// event изменения поля Team CustomField
def changeTeamEvent = event?.getChangeLog()?.getRelated("ChildChangeItem")?.find {it.field.toString() == "Team"}
if (changeTeamEvent){

// Получить список прилинкованных задач к задаче
// getInwardLinks
def issueKeys = []
List<IssueLink> ws = ComponentAccessor.getIssueLinkManager().getOutwardLinks(issue.id).each {
    def linkedIssue = it.destinationObject
    issueKeys.add("${linkedIssue.key}")
}
return issueKeys

// email field
emailFieldValue.contains('slotegrator.com')

//Current date
Date date = new Date()
String newDate = date.format("dd/MMM/yyyy")


//Last day current month
Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
