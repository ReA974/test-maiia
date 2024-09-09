package com.maiia.pro.service;

import com.maiia.pro.entity.Appointment;
import com.maiia.pro.entity.Availability;
import com.maiia.pro.repository.AppointmentRepository;
import com.maiia.pro.repository.AvailabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProAppointmentService {
    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    public Appointment find(String appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow();
    }

    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> findByPractitionerId(Integer practitionerId) {
        return appointmentRepository.findByPractitionerId(practitionerId);
    }

    public void generateAppointment(Appointment appointment) {
        if (appointment != null) {
            List<Appointment> existingAppointments = appointmentRepository.findByPractitionerId(appointment.getPractitionerId());
            List<Availability> availabilityList = availabilityRepository.findByPractitionerId(appointment.getPractitionerId());
            boolean duplicatedAppointement = existingAppointments.stream().anyMatch(existing ->
                    appointment.getStartDate().isBefore(existing.getEndDate()) && appointment.getEndDate().isAfter(existing.getStartDate())
            );
            boolean isAvailable = availabilityList.stream().anyMatch(availability ->
                    !appointment.getStartDate().isBefore(availability.getStartDate()) && !appointment.getEndDate().isAfter(availability.getEndDate())
            );

            if (!duplicatedAppointement && isAvailable) {
                appointmentRepository.save(appointment);

                availabilityRepository.deleteAll(
                        availabilityList.stream().filter(availability ->
                                availability.getStartDate().isBefore(appointment.getEndDate()) &&
                                        availability.getEndDate().isAfter(appointment.getStartDate())
                        ).collect(Collectors.toList())
                );
            } else {
                throw new RuntimeException("Can't create appointment");
            }
        }
    }
}
