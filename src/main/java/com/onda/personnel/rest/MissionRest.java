/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onda.personnel.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onda.personnel.rest.converter.DayDetailConverter;
import com.onda.personnel.rest.vo.DayDetailVo;
import com.onda.personnel.service.MissionService;

/**
 *
 * @author JaafarDiyaou
 */
@RestController
@CrossOrigin(origins = {"https://onda-marrakech.firebaseapp.com", "https://onda-menara.tk", "http://localhost:4200"})
@RequestMapping("/personnel-api/personnels/mission")
public class MissionRest {

    @Autowired
    MissionService missionService;

    @PutMapping("/")
    public int updateMission(@RequestBody DayDetailVo dayDetailVo) {
        return missionService.updateMission(new DayDetailConverter().toItem(dayDetailVo));
    }

    public MissionService getMissionService() {
        return missionService;
    }

    public void setMissionService(MissionService missionService) {
        this.missionService = missionService;
    }

}
