/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onda.personnel.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.onda.personnel.dao.DayDao;
import com.onda.personnel.model.Day;
import com.onda.personnel.model.DayDetail;
import com.onda.personnel.model.Detail;
import com.onda.personnel.model.Employee;
import com.onda.personnel.model.Holiday;
import com.onda.personnel.model.Timing;
import com.onda.personnel.model.Vacation;
import com.onda.personnel.model.Work;
import com.onda.personnel.model.WorkDetail;
import com.onda.personnel.service.DayDetailService;
import com.onda.personnel.service.DayService;
import com.onda.personnel.service.DetailService;
import com.onda.personnel.service.EmployeeService;
import com.onda.personnel.service.HolidayService;
import com.onda.personnel.service.VacationService;
import com.onda.personnel.service.WorkDetailService;
import com.onda.personnel.service.WorkService;
import com.onda.personnel.util.DateUtil;
import com.onda.personnel.util.PeriodUtil;
import com.onda.personnel.util.betweenDate;

/**
 * @author AMINE
 */
@Service
public class DayServiceImpl implements DayService {

    @Autowired
    private DayDao dayDao;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private WorkDetailService workDetailService;
    @Autowired
    private WorkService workService;
    @Autowired
    private DetailService detailService;

    @Autowired
    private DayDetailService dayDetailService;

    @Autowired
    private VacationService vacationService;

    @Autowired
    private HolidayService holidayService;

    @Override
    public int createDay(Integer matricule, List<Day> days) {
        Employee emp = employeeService.findByMatricule(matricule);
        if (emp == null) {
            return -1;
        } else if (days == null || days.isEmpty()) {
            return -2;
        } else if (days.size() > 7 || days.size() < 6) {
            return -3;
        } else {
            workDetailService.createWorkDetail(emp, days);
            return 1;
        }
    }

    @Override
    public Day setDayInfos(List<DayDetail> dayDetails, Date ld) {
        Day day = new Day();
        int pan = 0, minutesHnWorked = 0, minutesHeWorked = 0, hoursHnWorked = 0, hoursHeWorked = 0;
        day.setDayDate(ld);
        for (DayDetail dayDetail : dayDetails) {
            Detail dd = detailService.findByWording(dayDetail.getDetail().getWording());
            dayDetail.setDetail(dd);
            day.getDayDetails().add(dayDetailService.createDayDetail(dayDetail));
            if (!dd.getWording().equalsIgnoreCase("R")) {
                pan += dd.getPan();
                hoursHnWorked += dd.getHn().getHour();
                minutesHnWorked += dd.getHn().getMinute();
                hoursHeWorked += dd.getHe().getHour();
                minutesHeWorked += dd.getHe().getMinute();
                PeriodUtil.minutesToHour(hoursHnWorked, minutesHnWorked, hoursHeWorked, minutesHeWorked);
            }
        }
        day.setHn(new Timing(hoursHnWorked, minutesHnWorked));
        day.setHe(new Timing(hoursHeWorked, minutesHeWorked));
        day.setPan(pan);
        Holiday hol = holidayService.findByStartingDateLessThanEqualAndEndingDateGreaterThanEqual(ld, ld);

        if (hol != null) {
            day.setReference(hol.getReference());
        }
        dayDao.save(day);
        return day;
    }

    @Override
    public List<Day> findDaysOfWorkByEmployeeMatriculeAndYearAndMonth(Integer matricule, int year, int month) {
        List<Work> work = workService.findByEmployeeMatriculeAndMonthAndYear(matricule, year, month);
        return null;
    }

    @Override
    public Day findByEmployeeMatriculeAndDateOfTheDay(Integer matricule, Date dayDate) {
        LocalDate localDate = DateUtil.fromDate(dayDate);
        LocalDate checklocalDate = LocalDate.of(localDate.getYear(), localDate.getMonth(), 1);
        Date tmpDate = DateUtil.toDate(checklocalDate);
        Work work = workService.findByEmployeeMatriculeAndWorkDetailWorkDetailDate(matricule, tmpDate);
        if (work == null) {
            return null;
        } else {
            WorkDetail workDetail = work.getWorkDetail();
            List<Day> listOfDays = workDetail.getDays();
            Day theDay = new Day();
            for (Day day : listOfDays) {
                if (day.getDayDate().compareTo(dayDate) == 0) {
                    theDay = dayDao.getOne(day.getId());
                }
            }
            if (theDay == null) {
                return null;
            } else {
                return theDay;
            }
        }

    }

    @Override
    public int createVacation(Vacation vacation, Integer matricule) {
        int res = 0;
        Employee emp = employeeService.findByMatricule(matricule);
        LocalDate ldS = DateUtil.fromDate(vacation.getStartingDate());
        LocalDate ldE = DateUtil.fromDate(vacation.getEndingDate());

        if (emp == null) {
            return res = -1;
        } else if (vacation.getType().equals("C.M") || vacation.getType().equals("C.AT") || vacation.getType().equals("C.EXCEP")) {
            List<LocalDate> daysVacation = betweenDate.between(ldS, ldE);
            for (LocalDate ld : daysVacation) {
                Day day = findByEmployeeMatriculeAndDateOfTheDay(matricule, DateUtil.toDate(ld));
                vacation.setEmployee(emp);
                day.setVacation(vacation);
                vacationService.saveVacation(vacation);
            }
            return res = 1;

        } else if (vacation.getType().equals("C.R")) {
            List<LocalDate> daysVacationWithoutSunday = betweenDate.noSundays(ldS, ldE);
            for (LocalDate ld : daysVacationWithoutSunday) {
                Day day = findByEmployeeMatriculeAndDateOfTheDay(matricule, DateUtil.toDate(ld));
                vacation.setEmployee(emp);
                day.setVacation(vacation);
                vacationService.saveVacation(vacation);

            }
            return res = 2;
        }
        return res;
    }

    @Override
    public int updateVacation(Vacation vacation, Integer matricule) {
        int res = 0;
        Vacation oldVacation = vacationService.getVacationByID(vacation.getId());
        Employee emp = employeeService.findByMatricule(matricule);

        LocalDate ldS = DateUtil.fromDate(vacation.getStartingDate());
        LocalDate ldE = DateUtil.fromDate(vacation.getEndingDate());

        LocalDate ldSOld = DateUtil.fromDate(oldVacation.getStartingDate());
        LocalDate ldEOld = DateUtil.fromDate(oldVacation.getEndingDate());

        if (emp == null) {
            return res = -1;

        } else if (vacation.getType().equals("C.M") || vacation.getType().equals("C.AT") || vacation.getType().equals("C.EXCEP")) {
            List<LocalDate> daysVacation = betweenDate.between(ldS, ldE);
            List<LocalDate> daysVacationOld = betweenDate.between(ldSOld, ldEOld);
            daysVacation.removeAll(daysVacationOld);
            List<LocalDate> daysVacation2 = betweenDate.between(ldS, ldE);
            List<LocalDate> daysVacationOld2 = betweenDate.between(ldSOld, ldEOld);
            daysVacationOld2.removeAll(daysVacation2);
            oldVacation.setEmployee(emp);
            oldVacation.setEndingDate(vacation.getEndingDate());
            oldVacation.setStartingDate(vacation.getStartingDate());
            oldVacation.setType(vacation.getType());
            daysVacation.stream().map((localDate) -> findByEmployeeMatriculeAndDateOfTheDay(matricule, DateUtil.toDate(localDate))).map((day) -> {
                vacationService.saveVacation(oldVacation);
                day.setVacation(oldVacation);
                return day;
            }).forEachOrdered((day) -> {
                dayDao.save(day);
            });
            daysVacationOld2.stream().map((ld) -> findByEmployeeMatriculeAndDateOfTheDay(matricule, DateUtil.toDate(ld))).map((Noday) -> {
                Noday.setVacation(null);
                return Noday;
            }).forEachOrdered((Noday) -> {
                dayDao.save(Noday);
            });
            return res = 1;

        } else if (vacation.getType().equals("C.R")) {
            List<LocalDate> daysVacation = betweenDate.noSundays(ldS, ldE);
            List<LocalDate> daysVacationOld = betweenDate.noSundays(ldSOld, ldEOld);
            daysVacation.removeAll(daysVacationOld);
            List<LocalDate> daysVacation2 = betweenDate.noSundays(ldS, ldE);
            List<LocalDate> daysVacationOld2 = betweenDate.noSundays(ldSOld, ldEOld);
            daysVacationOld2.removeAll(daysVacation2);
            oldVacation.setEmployee(emp);
            oldVacation.setEndingDate(vacation.getEndingDate());
            oldVacation.setStartingDate(vacation.getStartingDate());
            oldVacation.setType(vacation.getType());
            daysVacation.stream().map((localDate) -> findByEmployeeMatriculeAndDateOfTheDay(matricule, DateUtil.toDate(localDate))).map((day) -> {
                vacationService.saveVacation(oldVacation);
                day.setVacation(oldVacation);
                return day;
            }).forEachOrdered((day) -> {
                dayDao.save(day);
            });
            daysVacationOld2.stream().map((ld) -> findByEmployeeMatriculeAndDateOfTheDay(matricule, DateUtil.toDate(ld))).map((Noday) -> {
                Noday.setVacation(null);
                return Noday;
            }).forEachOrdered((Noday) -> {
                dayDao.save(Noday);
            });
            return res = 2;
        }
        return res;
    }

    @Override
    public List<Day> findByDayDate(Date dayDate) {
        return dayDao.findByDayDate(dayDate);
    }

    @Override
    public List<Day> findByVacationId(Long id) {
        return dayDao.findByVacationId(id);
    }

    @Override
    public List<Day> findByDateOfTheWork(Date dateOfTheDay) {
        LocalDate localDate = DateUtil.fromDate(dateOfTheDay);
        LocalDate checkLocalDate = LocalDate.of(localDate.getYear(), localDate.getMonth(), 1);
        Date tmpDate = DateUtil.toDate(checkLocalDate);
        List<Day> listDay = new ArrayList<>();
        workService.findByWorkDetailWorkDetailDate(tmpDate).stream().map((work) -> work.getWorkDetail().getDays()).forEachOrdered((daysOfWork) -> {
            daysOfWork.stream().filter((day) -> (day.getDayDate().compareTo(dateOfTheDay) == 0)).forEachOrdered((day) -> {
                listDay.add(day);
            });
        });
        return listDay;
    }

    public DayDao getDayDao() {
        return dayDao;
    }

    public void setDayDao(DayDao dayDao) {
        this.dayDao = dayDao;
    }

    public EmployeeService getEmployeeService() {
        return employeeService;
    }

    public void setEmployeeService(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public WorkDetailService getWorkDetailService() {
        return workDetailService;
    }

    public void setWorkDetailService(WorkDetailService workDetailService) {
        this.workDetailService = workDetailService;
    }

    public WorkService getWorkService() {
        return workService;
    }

    public void setWorkService(WorkService workService) {
        this.workService = workService;
    }

    public DetailService getDetailService() {
        return detailService;
    }

    public void setDetailService(DetailService detailService) {
        this.detailService = detailService;
    }

    public DayDetailService getDayDetailService() {
        return dayDetailService;
    }

    public void setDayDetailService(DayDetailService dayDetailService) {
        this.dayDetailService = dayDetailService;
    }

    public VacationService getVacationService() {
        return vacationService;
    }

    public void setVacationService(VacationService vacationService) {
        this.vacationService = vacationService;
    }

    public HolidayService getHolidayService() {
        return holidayService;
    }

    public void setHolidayService(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

}
