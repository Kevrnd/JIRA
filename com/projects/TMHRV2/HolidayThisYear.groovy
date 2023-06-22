package com.slotegrator.projects.TMHRV2
import java.sql.Timestamp


//Вместе с датами сюда не забыть внести плавающие даты в темпо: https://jira.platform.live/secure/Tempo.jspa#/settings/holidays
//в 2023 в 2023 плавающие даты были толкьо у пасхи 7-10 апреля
public class  HolidayThisYear {
    public static List<Timestamp> holidays=
        [
        new Timestamp(123, 03, 07, 0, 0, 0, 0), //7 апреля
        new Timestamp(123, 03, 10, 0, 0, 0, 0), //10 апреля
        new Timestamp(123, 04, 01, 0, 0, 0, 0), //1 мая
        new Timestamp(123, 04, 8, 0, 0, 0, 0), //8 мая
        new Timestamp(123, 06, 05, 0, 0, 0, 0), //5 июля
        new Timestamp(123, 06, 06, 0, 0, 0, 0), //6 июля
        new Timestamp(123, 8, 28, 0, 0, 0, 0), //28 сентября
        new Timestamp(123, 10, 17, 0, 0, 0, 0), //17 ноября
        new Timestamp(123, 11, 25, 0, 0, 0, 0), //25 декабря
        new Timestamp(123, 11, 26, 0, 0, 0, 0), //26 декабря
        new Timestamp(122, 11, 28, 0, 0, 0, 0) // test holiday
        ]
    
}




