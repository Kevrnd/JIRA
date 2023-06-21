package com.slotegrator.projects.TMHRV2
import java.lang.Double
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.component.ComponentAccessor
import com.opensymphony.workflow.InvalidInputException
import java.sql.Timestamp
import java.util.Calendar
import com.slotegrator.projects.TMHRV2.HolidayThisYear



if (! (issue.issueType.name == 'Vacation' || issue.issueType.name == 'Sick day') ) {
    return 
}
List<Timestamp> holidays = new HolidayThisYear().holidays
def daysToDecrease = 0
BigDecimal hoursInWorkDay = 0 
ApplicationUser automationUser = ComponentAccessor.getUserManager().getUserByName("automation")
ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
String currentUserName = currentUser.getUsername()
String userGroupCheck = ComponentAccessor.groupManager.isUserInGroup(currentUser, 'Support Position')


Timestamp dueDate_date_timestamp = issue.getDueDate()
CustomFieldManager cfm = ComponentAccessor.getCustomFieldManager()
Object customFieldStartDate = cfm.getCustomFieldObject(11016L)
Object remainingVacationDays = cfm.getCustomFieldObject(13302L)  
Object remainingFamilyDays = cfm.getCustomFieldObject(13304L)  
Object customFieldTimeofAbsence = cfm.getCustomFieldObject(11100L)
Object remainingSickDay = cfm.getCustomFieldObject(13306L) 
Calendar calendar = Calendar.getInstance()
Timestamp start_date_timestamp = issue.getCustomFieldValue(customFieldStartDate)
def duedate_minus_start_date = issue.getDueDate() - start_date_timestamp

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def userCardSearchJQL = jqlQueryParser.parseQuery("project = AC and Username  ~ ${currentUserName} and status = 'In Progress'")
List<Issue> issues = ComponentAccessor.getComponent(SearchService).search(automationUser, userCardSearchJQL, PagerFilter.getUnlimitedFilter()).getResults()
Issue cardIssue = issues.get(0)
Double remainingVacationDaysValue = remainingVacationDays.getValue(cardIssue)
Double remainingFamilyDaysValue = remainingFamilyDays.getValue(cardIssue)
Double remainingSickDayValue = remainingSickDay.getValue(cardIssue)
calendar.setTime(start_date_timestamp)
int dayOfWeeks = calendar.get(Calendar.DAY_OF_WEEK)
BigDecimal timeOfAbsence = issue.getCustomFieldValue(customFieldTimeofAbsence) as BigDecimal
if (userGroupCheck == "true"){
    hoursInWorkDay = 7.5
} else {
    hoursInWorkDay = 8
}
if (issue.issueType.name == 'Vacation'){     
    if (duedate_minus_start_date == 0){
        if (!((dayOfWeeks == 1) || (dayOfWeeks == 7) || start_date_timestamp in holidays )) { // 1 - SUN; 7 SUT
            if ((remainingVacationDaysValue + remainingFamilyDaysValue)  < timeOfAbsence ){
                throw new InvalidInputException("Количество часов отпуска в задаче превышает имеющейся у вас остаток.")
            }
            if ( !((timeOfAbsence / hoursInWorkDay == 0.5) || (timeOfAbsence / hoursInWorkDay == 1))) {
                throw new InvalidInputException("Вы можете взять только половину дня или целый день отпуска.")
            }

        } else {
            throw new InvalidInputException("Вы хотите взять отпуск в выходной или праздничный день.")
        }
    } else {
        for (int i = 0; i <= duedate_minus_start_date; i++) {
            calendar.setTime(start_date_timestamp + i)
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if ((dayOfWeek == 1) || (dayOfWeek == 7) || start_date_timestamp + i in holidays ) { // 1 - SUN; 7 SUT
                continue
            }
            daysToDecrease++
        }
    }


    if (daysToDecrease > 0){
        //Проверка имеющегося остатка дней у пользователя с учетом календаря (Выходные и праздники не учитываются)
        if ((remainingVacationDaysValue + remainingFamilyDaysValue) <  (daysToDecrease * hoursInWorkDay - (hoursInWorkDay * 0.5) )) {
            throw new InvalidInputException("Количество дней отпуска в задаче превышает имеющейся у вас остаток.")
        }
        // Проверка заполнения поля при условии, что пользователь может взять X дней + 0.5 дня
        if ( !(timeOfAbsence == daysToDecrease * hoursInWorkDay || timeOfAbsence == daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5) ){
            throw new InvalidInputException('Проверьте правильность заполнения поля "Time of absence in hours"!')
        }
    }
}
// This block was added 2023-06-12 issue JS-7204 
if (issue.issueType.name == 'Sick day'){
    if (duedate_minus_start_date == 0){
        if (!((dayOfWeeks == 1) || (dayOfWeeks == 7) || start_date_timestamp in holidays )) { // 1 - SUN; 7 SUT
            if (remainingSickDayValue < timeOfAbsence ){
                throw new InvalidInputException("Количество часов Sick Days в задаче превышает имеющейся у вас остаток.")
            }
            log.warn("Вы можете взять только половину дня или целый день Sick day.  " + !((timeOfAbsence / hoursInWorkDay == 0.5) || (timeOfAbsence / hoursInWorkDay == 1)))
            if ( !((timeOfAbsence / hoursInWorkDay == 0.5) || (timeOfAbsence / hoursInWorkDay == 1))) {
                throw new InvalidInputException("Вы можете взять только половину дня или целый день Sick day.")
            }

        } else {
            throw new InvalidInputException("Вы хотите взять Sick day в выходной или праздничный день.")
        }
    } else {
        for (int i = 0; i <= duedate_minus_start_date; i++) {
            calendar.setTime(start_date_timestamp + i)
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if ((dayOfWeek == 1) || (dayOfWeek == 7) || start_date_timestamp + i in holidays ) { // 1 - SUN; 7 SUT
                continue
            }
            daysToDecrease++
        }
    
    }

    if (daysToDecrease > 0){
        log.warn ("daysToDecrease-->" + daysToDecrease + " hoursInWorkDay--> " + hoursInWorkDay)
        //Проверка имеющегося остатка дней у пользователя с учетом календаря (Выходные и праздники не учитываются)
        if ((remainingSickDayValue) <  (daysToDecrease * hoursInWorkDay - (hoursInWorkDay * 0.5) )) {
            throw new InvalidInputException("Количество дней Sick day в задаче превышает имеющейся у вас остаток.")
        }
        // Проверка заполнения поля при условии, что пользователь может взять X дней + 0.5 дня
        if ( !(timeOfAbsence == daysToDecrease * hoursInWorkDay || timeOfAbsence == daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5) ){
            throw new InvalidInputException('Проверьте правильность заполнения поля "Time of absence in hours"!')
        }
    }
}


