package com.slotegrator.projects.TMHRV2
import java.lang.String
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
Object customFieldStartDate = cfm.getCustomFieldObject(11016L)  // change
Object remainingVacationDays = cfm.getCustomFieldObject(13301L)   // change
Object remainingFamilyDays = cfm.getCustomFieldObject(13303L)     //change
Object customFieldTimeofAbsence = cfm.getCustomFieldObject(11100L)

Object remainingSickDay = cfm.getCustomFieldObject(13305L) 
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
BigDecimal timeOfAbsence = issue.getCustomFieldValue(customFieldTimeofAbsence) as BigDecimal;// change double

def daysOffInLeaveAndHolidays = ""//!!!!!!!!!!!!!!!!!!!\\\\\\\\\\!!!!!!!!!! Don't forget
String hoursSuffix(int hours) {
    if (hours % 10 == 1 && hours % 100 != 11) {
        return ""
    } else if ([2, 3, 4].contains(hours % 10) && ![12, 13, 14].contains(hours % 100)) {
        return "а"
    } else {
        return "ов"
    }
}

switch (issue.issueType.name){
    case 'Vacation' :
    // Для пользователей из группы Support
    if (userGroupCheck == "true"){
        hoursInWorkDay = 7.5
        //Проверка отпуска на один день
        if (duedate_minus_start_date == 0) {
            //Количество часов соотвествует  одному рабочему дню или половине исходя из группы пользователя
            if (timeOfAbsence / hoursInWorkDay != 0.5 && timeOfAbsence / hoursInWorkDay != 1) {
                throw new InvalidInputException("Вы можете взять только половину дня или целый день отпуска.")
            }
            // Проверяем, превышает ли запрошенное количество часов отпуска имеющийся остаток у пользователя
            if (remainingVacationDaysValue + remainingFamilyDaysValue < timeOfAbsence) {
                throw new InvalidInputException("Количество часов отпуска в задаче превышает имеющийся у вас остаток.")
            }
        } else if (duedate_minus_start_date > 0) {
            for (int i = 0; i <= duedate_minus_start_date; i++) {
                daysToDecrease++
            }
        }
        if (daysToDecrease >0) {
        // Проверка имеющегося остатка дней у пользователя
            if (remainingVacationDaysValue + remainingFamilyDaysValue < daysToDecrease * hoursInWorkDay - (hoursInWorkDay * 0.5)) {
                throw new InvalidInputException("Количество дней отпуска в задаче превышает имеющийся у вас остаток.")
            }
            // Проверка заполнения поля при условии, что пользователь может взять X дней + 0.5 дня
            if (!(timeOfAbsence == daysToDecrease * hoursInWorkDay || timeOfAbsence == daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5)) {
                String suffix = hoursSuffix((daysToDecrease * hoursInWorkDay- hoursInWorkDay * 0.5) as int)
                throw new InvalidInputException("Проверьте правильность заполнения поля 'Time of absence in hours'! \
                    Вы можете указать ${daysToDecrease * hoursInWorkDay}\
                    или ${daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5} час${suffix}.")

            }
        }
    // Для пользователей не входящих в группу Support
    } else {
        hoursInWorkDay = 8
        //Проверка отпуска на один день
        if (duedate_minus_start_date == 0) {
            if (dayOfWeeks == 1 || dayOfWeeks == 7 || start_date_timestamp in holidays) {
                throw new InvalidInputException("Вы хотите взять отпуск в выходной или праздничный день.")
            }

            if (timeOfAbsence / hoursInWorkDay != 0.5 && timeOfAbsence / hoursInWorkDay != 1) {
                throw new InvalidInputException("Вы можете взять только половину дня или целый день отпуска.")
            }
            // Проверяем, превышает ли запрошенное количество часов отпуска имеющийся остаток у пользователя
            if (remainingVacationDaysValue + remainingFamilyDaysValue < timeOfAbsence) {
                    throw new InvalidInputException("Количество часов отпуска в задаче превышает имеющийся у вас остаток.")
            }

        //Если отпуск не на один день
        } else if (duedate_minus_start_date > 0) {
            for (int i = 0; i <= duedate_minus_start_date; i++) {
                calendar.setTime(start_date_timestamp + i)
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == 1 || dayOfWeek == 7 || start_date_timestamp + i in holidays) {
                    daysOffInLeaveAndHolidays += (start_date_timestamp + i).format("dd/MMM/yyyy") as String + " , "
                    continue
                }
                daysToDecrease++
            }
        }

        if (daysToDecrease >= 0) {
        // Проверка имеющегося остатка дней у пользователя с учетом календаря (Выходные и праздники не учитываются)
            if (remainingVacationDaysValue + remainingFamilyDaysValue < daysToDecrease * hoursInWorkDay - (hoursInWorkDay * 0.5)) {
                throw new InvalidInputException("Количество дней отпуска в задаче превышает имеющийся у вас остаток.")
            }
        /*Данное сообщение пользователь увидит при условии указания всех дней отпуска в выходные или праздники 
        Актуально для тех ситуаций, когда  2 праздника подряд, и пользователь хочет взять на них отпуск
        Проверка заполнения поля при условии, что пользователь может взять X дней или X дней -0.5 дня
        */
            if (duedate_minus_start_date != 0 && daysToDecrease == 0 && !(timeOfAbsence == daysToDecrease * hoursInWorkDay || timeOfAbsence == daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5)) {
                String suffix = hoursSuffix((daysToDecrease * hoursInWorkDay) as int)
                throw new InvalidInputException("Проверьте правильность заполнения поля 'Time of absence in hours'! \
                    Вы можете указать ${daysToDecrease * hoursInWorkDay} час${suffix}.\
                    В указанных вами днях отпуска ${daysOffInLeaveAndHolidays} - являются праздником/выходным, \
                    поэтому они не учитываются")
            }
            if (!(timeOfAbsence == daysToDecrease * hoursInWorkDay || timeOfAbsence == daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5)) {
                String suffix = hoursSuffix((daysToDecrease * hoursInWorkDay- hoursInWorkDay * 0.5) as int)
                throw new InvalidInputException("Проверьте правильность заполнения поля 'Time of absence in hours'! \
                    Вы можете указать ${daysToDecrease * hoursInWorkDay}\
                    или ${daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5} час${suffix}.\
                    В указанных вами днях отпуска ${daysOffInLeaveAndHolidays} - являются праздником/выходным, \
                    поэтому они не учитываются.")
            }
        }
    }

    break

    case 'Sick day' :
    // Для пользователей из группы Support
    if (userGroupCheck == "true"){
        hoursInWorkDay = 7.5 
        throw new InvalidInputException("STOP TRUE")

    // Для пользователей не входящих в группу Support
    } else {
        hoursInWorkDay = 8
        //Проверка SickDay на один день
        if (duedate_minus_start_date == 0) {
            if (dayOfWeeks == 1 || dayOfWeeks == 7 || start_date_timestamp in holidays) {
                throw new InvalidInputException("Вы хотите взять SickDay в выходной или праздничный день.")
            }

            if (timeOfAbsence / hoursInWorkDay != 0.5 && timeOfAbsence / hoursInWorkDay != 1) {
                throw new InvalidInputException("Вы можете взять только половину дня или целый день Sick day.")
            }

            if (remainingSickDayValue < timeOfAbsence) {
                throw new InvalidInputException("Количество часов SickDay в задаче превышает имеющийся у вас остаток.")
            }
        //Если отпуск не на один день
        } else if (duedate_minus_start_date > 0) {
            for (int i = 0; i <= duedate_minus_start_date; i++) {
                calendar.setTime(start_date_timestamp + i)
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == 1 || dayOfWeek == 7 || start_date_timestamp + i in holidays) {
                    daysOffInLeaveAndHolidays += (start_date_timestamp + i).format("dd/MMM/yyyy") as String + " , "
                    continue
                }
                daysToDecrease++
            }
        }

        if (daysToDecrease >= 0) {
        // Проверка имеющегося остатка дней у пользователя с учетом календаря (Выходные и праздники не учитываются)
            if (remainingSickDayValue < daysToDecrease * hoursInWorkDay - (hoursInWorkDay * 0.5)) {
                throw new InvalidInputException("Количество дней SickDay в задаче превышает имеющийся у вас остаток.")
            }
        /*Данное сообщение пользователь увидит при условии указания всех дней отпуска в выходные или праздники 
        Актуально для тех ситуаций, когда  2 праздника подряд, и пользователь хочет взять на них отпуск
        Проверка заполнения поля при условии, что пользователь может взять X дней или X дней -0.5 дня
        */
            if (duedate_minus_start_date != 0 && daysToDecrease == 0 && !(timeOfAbsence == daysToDecrease * hoursInWorkDay || timeOfAbsence == daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5)) {
                String suffix = hoursSuffix((daysToDecrease * hoursInWorkDay) as int)
                throw new InvalidInputException("Проверьте правильность заполнения поля 'Time of absence in hours'! \
                    Вы можете указать ${daysToDecrease * hoursInWorkDay} час${suffix}.\
                    В указанных вами днях Sick days ${daysOffInLeaveAndHolidays} - являются праздником/выходным, \
                    поэтому они не учитываются")
            }
            if (!(timeOfAbsence == daysToDecrease * hoursInWorkDay || timeOfAbsence == daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5)) {
                String suffix = hoursSuffix((daysToDecrease * hoursInWorkDay- hoursInWorkDay * 0.5) as int)
                throw new InvalidInputException("Проверьте правильность заполнения поля 'Time of absence in hours'! \
                    Вы можете указать ${daysToDecrease * hoursInWorkDay}\
                    или ${daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5} час${suffix}.\
                    В указанных вами днях Sick day ${daysOffInLeaveAndHolidays} - являются праздником/выходным, \
                    поэтому они не учитываются.")
            }
        }
    }
    break 

}





 

/* Old version

    if (duedate_minus_start_date == 0){
        if (!((dayOfWeeks == 1) || (dayOfWeeks == 7) || start_date_timestamp in holidays )) { // 1 - SUN; 7 SUT
            if ((remainingVacationDaysValue + remainingFamilyDaysValue)  < timeOfAbsence ){
                throw new InvalidInputException("Количество дней отпуска в задаче превышает имеющейся у вас остаток.")
            }
            log.error("timeOfAbsence / hoursInWorkDay" + !((timeOfAbsence / hoursInWorkDay == 0.5) || (timeOfAbsence / hoursInWorkDay == 1)))
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
         
                
                daysOffInLeaveAndHolidays += (start_date_timestamp + i).format("dd/MMM/yyyy") as String + " , " 
                continue
            }
            daysToDecrease++
        }
    }


    if (daysToDecrease >= 0){
     //Проверка имеющегося остатка дней у пользователя с учетом календаря (Выходные и праздники не учитываются)
        if ((remainingVacationDaysValue + remainingFamilyDaysValue) <  (daysToDecrease * hoursInWorkDay - (hoursInWorkDay * 0.5) )) {
            throw new InvalidInputException("Количество дней отпуска в задаче превышает имеющейся у вас остаток.")
        }
        // Проверка заполнения поля при условии, что пользователь может взять X дней + 0.5 дня
        if ( !(timeOfAbsence == daysToDecrease * hoursInWorkDay || timeOfAbsence == daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5) ){
            throw new InvalidInputException("Проверьте правильность заполнения поля 'Time of absence in hours'! \
            Вы можете указать ${daysToDecrease* hoursInWorkDay}\
            или ${daysToDecrease *hoursInWorkDay- hoursInWorkDay * 0.5} часов(а)\
            В указанных вами днях отпуска ${daysOffInLeaveAndHolidays} - являются праздником/выходным, \
            поэтому они не учитываются")
        }
    }
} 







if (issue.issueType.name == 'Sick day'){
    if (duedate_minus_start_date == 0){
        if (!((dayOfWeeks == 1) || (dayOfWeeks == 7) || start_date_timestamp in holidays )) { // 1 - SUN; 7 SUT
            if ((remainingSickDayValue)  < timeOfAbsence ){
                throw new InvalidInputException("Количество дней Sick Days в задаче превышает имеющейся у вас остаток.")
            }
            log.error("timeOfAbsence / hoursInWorkDay" + !((timeOfAbsence / hoursInWorkDay == 0.5) || (timeOfAbsence / hoursInWorkDay == 1)))
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
*/

