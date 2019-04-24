/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onda.personnel.service.impl;

import com.onda.personnel.bean.Day;
import com.onda.personnel.bean.Employee;
import com.onda.personnel.bean.Work;
import com.onda.personnel.bean.WorkDetail;
import com.onda.personnel.common.util.DateUtil;
import com.onda.personnel.dao.WorkDetailDao;
import com.onda.personnel.service.EmployeeService;
import com.onda.personnel.service.WorkDetailSevice;
import com.onda.personnel.service.WorkService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author AMINE
 */
@Service
public class WorkDetailSeviceImpl implements WorkDetailSevice {

    @Autowired
    private WorkService workService;
    @Autowired
    private WorkDetailDao workDetailDao;
    @Autowired
    private EmployeeService employeeService;

    private static final Logger log = LoggerFactory.getLogger(WorkDetailSeviceImpl.class);

    /*@Override
    public List<WorkDetail> findByWorkDetailDate(LocalDate workDetailDate) {
        if (workDetailDao.findByWorkDetailDate(workDetailDate).isEmpty()) {
            return null;
        } else {
            return workDetailDao.findByWorkDetailDate(workDetailDate);
        }
    }*/
    @Override
    public WorkDetail findByWorkDetailDate(LocalDate localDate) {
        return workDetailDao.findByWorkDetailDate(localDate);
    }

    @Override
    public void saveWorkDetail(WorkDetail workDetail) {
        workDetailDao.save(workDetail);
    }

    @Override
    public void createWorkDetail(Employee emp, List<Day> days) {
        Work work = workService.findTopByEmployeeMatriculeOrderByWorkDetailTestDateDesc(emp.getMatricule());
        //WorkDetail workDetail = findByWorkDetailDate(workDetailDate);
        WorkDetail workDetail;
        int workDetailListLength = DateUtil.fromDate(new Date()).lengthOfMonth();
        if (work == null || work.getWorkDetail() == null) {
            work = new Work();
            work.setEmployee(emp);
            workDetail = new WorkDetail(new ArrayList(), new Date(), 0, 0, 0);
            workDetail.setWorkDetailDate(DateUtil.toDate(DateUtil.getFirstMonday(DayOfWeek.MONDAY)));
            workDetailListLength = workDetailListLength - DateUtil.getFirstMonday(DayOfWeek.MONDAY).getDayOfMonth() + 1;
        } else {
            workDetail = workDetailDao.getOne(work.getWorkDetail().getId());
        }
        LocalDate ld = DateUtil.fromDate(new Date(workDetail.getWorkDetailDate().getYear(),workDetail.getWorkDetailDate().getMonth(),1)).plusMonths(1);
        WorkDetail newWorkDetail = new WorkDetail(new ArrayList(), DateUtil.toDate(ld), 0, 0, 0);
        try {
            for (Day day : days) {
                if (workDetailListLength > workDetail.getDays().size()) {
                    setOtherInfos(workDetail, day);
                    workDetail.getDays().add(day);
                } else {
                    setOtherInfos(newWorkDetail, day);
                    newWorkDetail.getDays().add(day);
                }
            }
        } catch (NullPointerException e) {
            log.error("null sur le bloc boucle for " + e.getMessage());
        }

        if (newWorkDetail.getDays() == null || newWorkDetail.getDays().isEmpty()) {
            saveWorkDetail(workDetail);
            work.setWorkDetail(workDetail);
            workService.saveWork(work);
        } else {
            Work newWork = new Work();
            saveWorkDetail(workDetail);
            saveWorkDetail(newWorkDetail);
            newWork.setEmployee(emp);
            newWork.setWorkDetail(newWorkDetail);
            workService.saveWork(work);
            workService.saveWork(newWork);
        }
    }

    private void setOtherInfos(WorkDetail workDetail, Day day) {
        workDetail.setPan(workDetail.getPan() + day.getPan());
        workDetail.setHn(workDetail.getHn() + day.getHn());
        workDetail.setHjf(workDetail.getHjf() + day.getHe());
    }

    public WorkDetailDao getWorkDetailDao() {
        return workDetailDao;
    }

    public void setWorkDetailDao(WorkDetailDao workDetailDao) {
        this.workDetailDao = workDetailDao;
    }

    public EmployeeService getEmployeeService() {
        return employeeService;
    }

    public void setEmployeeService(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public WorkService getWorkService() {
        return workService;
    }

    public void setWorkService(WorkService workService) {
        this.workService = workService;
    }
}
