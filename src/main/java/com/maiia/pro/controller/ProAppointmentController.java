package com.maiia.pro.controller;

import com.maiia.pro.dto.AppointmentDTO;
import com.maiia.pro.entity.Appointment;
import com.maiia.pro.service.ProAppointmentService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = "/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProAppointmentController {
    @Autowired
    private ProAppointmentService proAppointmentService;

    @ApiOperation(value = "Get appointments by practitionerId")
    @GetMapping("/{practitionerId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByPractitioner(@PathVariable final Integer practitionerId) {
        return new ResponseEntity<>(proAppointmentService.findByPractitionerId(practitionerId), HttpStatus.OK);
    }

    @ApiOperation(value = "Get all appointments")
    @GetMapping
    public ResponseEntity<List<Appointment>> getAppointments() {
        return new ResponseEntity<>(proAppointmentService.findAll(), HttpStatus.OK);
    }

    @ApiOperation(value = "Create an appointment")
    @PostMapping
    public ResponseEntity<String> generateAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        try {
            Appointment appointment = new Appointment();
            appointment.setId(appointmentDTO.getId());
            appointment.setPractitionerId(appointmentDTO.getPractitionerId());
            appointment.setPatientId(appointmentDTO.getPatientId());
            appointment.setStartDate(appointmentDTO.getStartDate());
            appointment.setEndDate(appointmentDTO.getEndDate());
            proAppointmentService.generateAppointment(appointment);
            return new ResponseEntity<>("Appointment created successfully", HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
